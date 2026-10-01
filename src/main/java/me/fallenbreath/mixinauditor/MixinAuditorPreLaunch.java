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

package me.fallenbreath.mixinauditor;

import me.fallenbreath.mixinauditor.impl.AuditConfigFilter;
import me.fallenbreath.mixinauditor.impl.MixinAuditor;
import me.fallenbreath.mixinauditor.impl.MixinExclusion;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Applies the config filter before the game classes start loading.
 *
 * <p>Doing it here rather than when the audit runs is what makes the filter able to keep a broken
 * dependency mixin out of the way. Such mixins fail while their target class is loaded, which is
 * before the audit is ever triggered.</p>
 */
public class MixinAuditorPreLaunch implements PreLaunchEntrypoint
{
	@Override
	public void onPreLaunch()
	{
		if (!MixinAuditor.isEnabled())
		{
			return;
		}

		AuditConfigFilter.applyToRegistration();
		MixinExclusion.install();
	}
}
