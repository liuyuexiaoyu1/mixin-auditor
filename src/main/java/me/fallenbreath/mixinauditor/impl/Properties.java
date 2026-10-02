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

public class Properties
{
	public static final String SWITCH = "mixinAuditor.audit";
	public static final String WHEN = "mixinAuditor.when";
	public static final String EXIT = "mixinAuditor.exit";
	public static final String FAIL_CODE = "mixinAuditor.failCode";
	// comma separated mixin config name prefixes to restrict the audit to, empty means audit all
	public static final String CONFIG_FILTER = "mixinAuditor.configFilter";
	// comma separated mixin class name prefixes to keep out of the mixin application entirely,
	// empty means drop nothing
	public static final String EXCLUDE = "mixinAuditor.excludeMixins";
	// comma separated mixin class name prefixes to keep in the mixin application, everything else
	// is dropped, empty means keep everything
	public static final String KEEP = "mixinAuditor.keepMixins";
}
