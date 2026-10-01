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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class MixinAuditor
{
	private static final Logger LOGGER = LogManager.getLogger(MixinAuditor.class);
	private static final int DEFAULT_FAIL_CODE = 19;

	public static boolean isEnabled()
	{
		return "true".equalsIgnoreCase(System.getProperty(Properties.SWITCH));
	}

	public static int getFailCode()
	{
		String codeStr = System.getProperty(Properties.FAIL_CODE, "");
		try
		{
			return Integer.parseInt(codeStr);
		}
		catch (NumberFormatException e)
		{
			return DEFAULT_FAIL_CODE;
		}
	}

	public static void run()
	{
		LOGGER.info("Triggered auto mixin audit");
		boolean ok = doAudit();

		boolean exit;
		switch (ExitMode.get())
		{
			case TRUE:
				exit = true;
				break;
			case FALSE:
				exit = false;
				break;
			case ON_FAIL:
				exit = !ok;
				break;
			default:
				throw new RuntimeException();
		}

		if (exit)
		{
			System.exit(ok ? 0 : getFailCode());
		}
	}

	private static boolean doAudit()
	{
		try
		{
			MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
			AuditConfigFilter.apply(environment);
			environment.audit();
		}
		catch (Throwable t)
		{
			LOGGER.error("Error when auditing mixin", t);
			return false;
		}

		LOGGER.info("Mixin environment audited successfully");
		return true;
	}
}
