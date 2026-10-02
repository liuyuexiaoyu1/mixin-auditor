/*
 * This file is part of the Mixin Auditor project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2025  Fallen_Breath and contributors
 *
 * Mixin Auditor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mixin Auditor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mixin Auditor.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.fallenbreath.mixinauditor.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

/**
 * Keeps the selected mixins out of the mixin application entirely.
 *
 * <p>An audit may be run in an environment whose mods do not match the audited game version: a
 * dependency mixin whose injection points no longer exist blows up while its target class is
 * being transformed, which happens before the audit is ever triggered. Such a mixin can be
 * removed right before it is applied.</p>
 *
 * <p>MixinProcessor collects every MixinInfo for a target class into the TargetClassContext it
 * creates, and calls applyMixins on it only afterwards. IExtension#preApply runs in between and
 * the collected set is a plain sorted collection, so removing entries here keeps them from ever
 * being applied.</p>
 *
 * <p>Two properties drive the selection. "mixinAuditor.excludeMixins" lists class name prefixes
 * to drop, which means naming every dependency that gets in the way. "mixinAuditor.keepMixins"
 * lists the prefixes to keep instead and drops everything else, which is usually a single entry
 * since a project's own mixins share a package. The exclude list wins when both match.</p>
 */
public class MixinExclusion implements IExtension
{
	private static final Logger LOGGER = LogManager.getLogger(MixinExclusion.class);
	private static final String TARGET_CONTEXT_CLASS = "org.spongepowered.asm.mixin.transformer.TargetClassContext";

	private final List<String> dropPrefixes;
	private final List<String> keepPrefixes;

	private MixinExclusion(List<String> dropPrefixes, List<String> keepPrefixes)
	{
		this.dropPrefixes = dropPrefixes;
		this.keepPrefixes = keepPrefixes;
	}

	/**
	 * Install the exclusion extension. Does nothing when neither list is configured.
	 */
	public static void install()
	{
		List<String> dropPrefixes = readPrefixes(Properties.EXCLUDE);
		List<String> keepPrefixes = readPrefixes(Properties.KEEP);
		if (dropPrefixes.isEmpty() && keepPrefixes.isEmpty())
		{
			return;
		}

		try
		{
			IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
			if (transformer == null)
			{
				LOGGER.warn("No active mixin transformer, the mixin exclusion is not installed");
				return;
			}

			// go through the public IMixinTransformer interface: MixinTransformer itself is package
			// private, so reflecting on its members from here trips the access check
			Extensions extensions = (Extensions) transformer.getExtensions();
			extensions.add(new MixinExclusion(dropPrefixes, keepPrefixes));
			// add() only appends to the full list; preApply iterates activeExtensions, which select()
			// rebuilds. Re-run it so the new extension is actually called.
			extensions.select(MixinEnvironment.getDefaultEnvironment());

			if (!dropPrefixes.isEmpty())
			{
				LOGGER.info("Mixin exclusion drops mixins matching: {}", String.join(", ", dropPrefixes));
			}
			if (!keepPrefixes.isEmpty())
			{
				LOGGER.info("Mixin exclusion keeps only mixins matching: {}", String.join(", ", keepPrefixes));
			}
		}
		catch (Throwable t)
		{
			LOGGER.error("Failed to install the mixin exclusion", t);
		}
	}

	@Override
	public boolean checkActive(MixinEnvironment environment)
	{
		return true;
	}

	@Override
	public void preApply(ITargetClassContext context)
	{
		if (!TARGET_CONTEXT_CLASS.equals(context.getClass().getName()))
		{
			return;
		}

		try
		{
			Field field = findField(context.getClass(), "mixins");
			Object value = field.get(context);
			if (!(value instanceof SortedSet))
			{
				return;
			}

			SortedSet<IMixinInfo> mixins = castMixins(value);
			List<String> excluded = new ArrayList<>();

			mixins.removeIf(mixin -> {
				String mixinClassName = mixin.getClassName();
				if (shouldExclude(mixinClassName))
				{
					excluded.add(mixinClassName);
					return true;
				}
				return false;
			});

			if (!excluded.isEmpty())
			{
				LOGGER.info("Kept {} mixin(s) out of the application: {}", excluded.size(), String.join(", ", excluded));
			}
		}
		catch (Throwable t)
		{
			LOGGER.error("Failed to apply the mixin exclusion", t);
		}
	}

	@Override
	public void postApply(ITargetClassContext context)
	{
	}

	@Override
	public void export(MixinEnvironment environment, String name, boolean force, ClassNode classNode)
	{
	}

	private boolean shouldExclude(String mixinClassName)
	{
		if (mixinClassName == null)
		{
			return false;
		}

		if (matches(dropPrefixes, mixinClassName))
		{
			return true;
		}

		// with a keep list configured, anything not matching it is dropped
		return !keepPrefixes.isEmpty() && !matches(keepPrefixes, mixinClassName);
	}

	private static boolean matches(List<String> prefixes, String mixinClassName)
	{
		for (String prefix : prefixes)
		{
			if (mixinClassName.startsWith(prefix))
			{
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private static SortedSet<IMixinInfo> castMixins(Object value)
	{
		return (SortedSet<IMixinInfo>) value;
	}

	private static Field findField(Class<?> owner, String name) throws NoSuchFieldException
	{
		for (Class<?> cls = owner; cls != null; cls = cls.getSuperclass())
		{
			try
			{
				Field field = cls.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException ignored)
			{
				// keep looking up the hierarchy
			}
		}

		throw new NoSuchFieldException(name + " in " + owner.getName());
	}

	private static List<String> readPrefixes(String property)
	{
		String raw = System.getProperty(property, "");
		List<String> prefixes = new ArrayList<>();

		for (String part : raw.split(","))
		{
			String prefix = part.trim();
			if (!prefix.isEmpty())
			{
				prefixes.add(prefix);
			}
		}

		return prefixes;
	}
}
