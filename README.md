# mixin-auditor

[![maven badge](https://maven.fallenbreath.me/api/badge/latest/releases/me/fallenbreath/mixin-auditor)](https://maven.fallenbreath.me/#/releases/me/fallenbreath/mixin-auditor)

A tiny fabric mod to automatically trigger a `MixinEnvironment.getCurrentEnvironment().audit()` on Minecraft launch

It is available at [my maven](https://maven.fallenbreath.me/#/releases/me/fallenbreath/mixin-auditor)

It requires no dependencies, and should work in all Minecraft versions >= 1.14

## Usages

### Import

```groovy
repositories {
    maven { url = 'https://maven.fallenbreath.me/releases' }
}

dependencies {
    // obfuscated Minecraft versions (MC < 26.1): 
    modRuntimeOnly 'me.fallenbreath:mixin-auditor:0.2.0-o'

    // unobfuscated Minecraft versions (MC >= 26.1): 
    runtimeOnly 'me.fallenbreath:mixin-auditor:0.2.0-u'
}
```

Normally mixin auditor should only be used in development environment, that's why `modRuntimeOnly` is use here

### Run

Run Minecraft with system property `-DmixinAuditor.audit=true`, and mixin auditor will handle the rest

Yeah, not with config file / API, cuz it's designed for development / CI environment, not for users in production
environment

Here's a simple examples to launch the server with mixin auditor enabled

```bash
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -DmixinAuditor.audit=true"
./gradlew runServer
```

### Loom

You can create a new loom run config for better integration with your gradle project

```gradle
loom {
    runs {
        // run server, but with mixin auditor enabled
        serverMixinAudit {
            server()
            vmArgs '-DmixinAuditor.audit=true'
        }
        
        // run client, but with mixin auditor enabled
        clientMixinAudit {
            client()
            vmArgs('-DmixinAuditor.audit=true', '-DmixinAuditor.when=game_init')
        }
    }
}
```

Then you can use the following gradle command to launch Minecraft server with mixin auditor enabled:

```bash
./gradlew runServerMixinAudit
./gradlew runClientMixinAudit
```

## Config

Mixin auditor can be configured with java system property

Available properties:

| Property                | Description                                                                                                                                                                               |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mixinAuditor.audit`    | The main switch. Set it to `true` to enable mixin auditor, otherwise mixin auditor will do nothing                                                                                        |
| `mixinAuditor.when`     | When will mixin auditor triggers. Options: `mod_init`, fabric's `ModInitializer#onInitialize` hook; `game_init`, when the game is initialized and is about to start . Default: `mod_init` |
| `mixinAuditor.exit`     | If the Minecraft process should exit after auditing. Options: `true`, `false`, `on_fail`. Default: `true`                                                                                 |
| `mixinAuditor.failCode` | The return code to be used on exit if audit failed. It should be a valid integer. Default: `19`                                                                                           |
| `mixinAuditor.configFilter` | Comma separated mixin config name prefixes to restrict the audit to, e.g. `carpet-igny-addition`. Empty means audit every config. Default: empty                                      |

### Filtering configs

By default the audit covers every mixin config registered in the current environment, including
those of your dependencies. That is noisy, and since the audit aborts on the first failure, a
broken mixin of some other mod can hide every failure of your own.

Set `mixinAuditor.configFilter` to a comma separated list of config name prefixes to keep only
the matching configs. For the usual `&lt;modid&gt;.mixins.json` layout a prefix is just the mod
id:

```gradle
loom {
    runs {
        clientMixinAudit {
            client()
            vmArgs '-DmixinAuditor.audit=true', '-DmixinAuditor.when=game_init'
            vmArgs '-DmixinAuditor.configFilter=carpet-igny-addition,some-other-mod'
        }
    }
}
```

Note the property has to reach the *game* JVM, so declare it via the run config as above. A
`-D` on the gradle command line only configures the gradle daemon and will silently do nothing.
Exporting `JAVA_TOOL_OPTIONS` also works, since the forked game JVM inherits it.

Matching is by name prefix, and if nothing matches you get a warning instead of a silently
empty audit.

The filter is applied in two places. A `preLaunch` entrypoint drops the excluded configs from
the registration set before the mixin subsystem ever prepares them, so their mixins are never
applied at all, and a second pass right before auditing covers anything that slipped through.
Filtering that early is what makes the option usable when a dependency mixin is broken: such a
mixin dies while the class holding its target is being loaded, which happens long before an
audit is triggered and is out of reach of a filter applied at audit time. Note that with the
filter active those mods really are inert for the run, which is intended — the point is to audit
your own mixins without a dependency's breakage getting in the way.
