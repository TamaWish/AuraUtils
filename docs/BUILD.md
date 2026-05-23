# Building AuraUtils

This guide covers compiling, packaging, and deploying the plugin JAR. For gameplay setup, see [README.md](../README.md).

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|--------|
| **JDK** | 25+ | Must match `pom.xml` (`java.version` / compiler source & target) |
| **Maven** | 3.9+ | Bundled under `.maven/apache-maven-3.9.16/` in this repo |
| **Network** | — | First build downloads Spigot API and dependencies from Maven Central / Spigot repo |

Check your environment:

```powershell
java -version
```

```powershell
.\.maven\apache-maven-3.9.16\bin\mvn.cmd -version
```

## Quick build

From the repository root:

**Windows (bundled Maven):**

```powershell
.\.maven\apache-maven-3.9.16\bin\mvn.cmd clean package
```

**Any OS (Maven on PATH):**

```bash
mvn clean package
```

On success, the deployable artifact is:

```
target/AuraUtils-1.0.0.jar
```

Copy that file to your server `plugins/` folder and **restart** the server.

## Build output

| File | Deploy to server? | Description |
|------|-------------------|-------------|
| `target/AuraUtils-1.0.0.jar` | **Yes** | Shaded plugin JAR (Adventure/MiniMessage relocated to `me.aurautils.lib.kyori`) |
| `target/original-AuraUtils-1.0.0.jar` | No | Classes only, before shading — missing bundled dependencies |
| `target/classes/` | No | Compiled `.class` files for development |

The **maven-shade-plugin** runs at the `package` phase and **replaces** the main artifact. There is no separate `*-shaded.jar` filename.

## What the build does

1. **compile** — `src/main/java` → `target/classes` (Java 25, forked `javac`)
2. **resources** — `src/main/resources` → `target/classes` (filtered `plugin.yml` with `${project.version}`)
3. **jar** — packages classes and resources
4. **shade** — Relocates `net.kyori.*` into the plugin JAR for server use without extra libs

## Deploying to a server

1. Stop the server (or remove the old JAR before start).
2. Delete any duplicate `AuraUtils*.jar` in `plugins/` to avoid version conflicts.
3. Copy **`AuraUtils-1.0.0.jar`** only.
4. Start the server.
5. Confirm console shows `AuraUtils v1.0.0 enabled` without errors.

Use a **full restart** after upgrades. Reload plugins are not recommended when replacing the JAR.

## IDE vs Maven

Always ship a JAR built with **Maven** (`mvn clean package`).

If your IDE (Eclipse, IntelliJ, VS Code Java) compiles while the workspace has syntax errors, it can emit `.class` files that throw at runtime:

```text
java.lang.Error: Unresolved compilation problems
    at me.aurautils.menus.MenuManager.<init>(MenuManager.java:129)
```

That is not a Spigot bug — it means broken bytecode was packaged. Fix compile errors, run `mvn clean package`, and redeploy.

**Recommended IDE workflow:**

- Let Maven own compilation for releases.
- After pulling changes, run `clean package` before copying to a test server.
- Do not copy from IDE `out/`, `bin/`, or “Export JAR” unless you verify a clean Maven build first.

## Common commands

| Goal | Command |
|------|---------|
| Clean + build | `mvn clean package` |
| Compile only | `mvn compile` |
| Skip tests (default: no tests) | `mvn clean package` |
| Offline (deps cached) | `mvn -o clean package` |
| Verbose errors | `mvn clean package -e` |

## Troubleshooting

### `mvn` is not recognized

Use the bundled wrapper path (Windows):

```powershell
.\.maven\apache-maven-3.9.16\bin\mvn.cmd clean package
```

Or install [Apache Maven](https://maven.apache.org/download.cgi) and add `bin` to your `PATH`.

### Unsupported class file major version

The server or JDK used to **run** Maven must be **25+**. Upgrade the JDK used for `java -version` and `JAVA_HOME`.

### Spigot API download fails

Ensure network access to `https://hub.spigotmc.org/nexus/`. Corporate proxies may need Maven `settings.xml` proxy configuration.

### Plugin enables but features fail / old behavior

- Confirm only one `AuraUtils-1.0.0.jar` in `plugins/`.
- Rebuild with `clean` so stale classes are removed: `mvn clean package`.
- Check file timestamp on the JAR you copied.

### Shade / signature warnings

Maven may warn about overlapping `META-INF` entries from dependencies. The build excludes signing files in the shade config; warnings are usually safe to ignore unless the JAR fails to load.

## Version alignment

These should stay in sync when bumping a release:

| Location | Field |
|----------|--------|
| `pom.xml` | `<version>` |
| `plugin.yml` | `${project.version}` (filtered at build) |
| `README.md` / `CHANGELOG.md` | Documented version and date |

After changing the version in `pom.xml`, run `mvn clean package` and update [CHANGELOG.md](../CHANGELOG.md).

## CI example

GitHub Actions (illustrative):

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: "25"
          distribution: "temurin"
      - run: mvn -B clean package
      - uses: actions/upload-artifact@v4
        with:
          name: AuraUtils-jar
          path: target/AuraUtils-*.jar
          exclude: original-*
```

## See also

- [README.md](../README.md) — features, config, permissions
- [CHANGELOG.md](../CHANGELOG.md) — version history
- [LICENSE](../LICENSE) — MIT license terms
