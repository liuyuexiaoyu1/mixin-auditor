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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

/**
 * Restricts the audit to a subset of the registered mixin configs.
 *
 * <p>MixinEnvironment::audit delegates to MixinTransformer, which walks every MixinConfig it
 * has collected and force-loads each target class so that pending mixins get applied (and any
 * broken one blows up). A config that has already been collected cannot be excluded by touching
 * MixinEnvironment::getMixinConfigs, since MixinProcessor copied it into its own list when the
 * mixin subsystem started. That list is what the audit actually iterates, and it is an
 * ArrayList, so dropping entries from it here is enough.</p>
 *
 * <p>Configs are matched by name prefix, which for the usual "&lt;modid&gt;.mixins.json" layout
 * amounts to filtering by mod. With no prefix configured the audit behaves exactly as before.</p>
 */
public class AuditConfigFilter
{
	private static final Logger LOGGER = LogManager.getLogger(AuditConfigFilter.class);

	/**
	 * Drop every collected mixin config that is not covered by the configured prefixes.
	 *
	 * @param environment environment whose audit is about to run
	 */
	public static void apply(MixinEnvironment environment)
	{
		List<String> prefixes = getPrefixes();
		if (prefixes.isEmpty())
		{
			return;
		}

		Object transformer = environment.getActiveTransformer();
		if (transformer == null)
		{
			LOGGER.warn("No active mixin transformer, the mixin config filter is not applied");
			return;
		}

		try
		{
			List<?> configs = getCollectedConfigs(transformer);
			int total = configs.size();
			configs.removeIf(config -> !matches(prefixes, ((IMixinConfig) config).getName()));
			int kept = configs.size();

			LOGGER.info("Mixin config filter '{}' audits {} of {} configs", String.join(", ", prefixes), kept, total);
			if (kept == 0)
			{
				LOGGER.warn("The mixin config filter '{}' matched no config, the audit below is a no-op", String.join(", ", prefixes));
			}
		}
		catch (Throwable t)
		{
			LOGGER.error("Failed to apply the mixin config filter, auditing every config", t);
		}
	}

	/**
	 * Read the config list MixinProcessor collected for the current environment.
	 *
	 * <p>Reached reflectively because MixinProcessor and MixinTransformer are package private in
	 * the mixin subsystem, so they cannot be referenced at compile time from here.</p>
	 */
	private static List<?> getCollectedConfigs(Object transformer) throws ReflectiveOperationException
	{
		Field processorField = findField(transformer.getClass(), "processor");
		Object processor = processorField.get(transformer);

		Field configsField = findField(processor.getClass(), "configs");
		return (List<?>) configsField.get(processor);
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

	/**
	 * Prefixes from the "mixinAuditor.configFilter" property, comma separated. Empty means audit
	 * everything.
	 */
	private static List<String> getPrefixes()
	{
		String raw = System.getProperty(Properties.CONFIG_FILTER, "");
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

	private static boolean matches(List<String> prefixes, String configName)
	{
		if (configName == null)
		{
			return false;
		}

		for (String prefix : prefixes)
		{
			if (configName.startsWith(prefix))
			{
				return true;
			}
		}

		return false;
	}
}
