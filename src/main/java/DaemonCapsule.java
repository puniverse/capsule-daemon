/*
 * Capsule
 * Copyright (c) 2015, Parallel Universe Software Co. and Contributors. All rights reserved.
 *
 * This program and the accompanying materials are licensed under the terms
 * of the Eclipse Public License v1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
import co.paralleluniverse.capsule.daemon.DaemonAdapter;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A caplet that will use jsvc (needs to be installed) to launch the application as an Unix daemon. Several configuration options are provided.
 *
 * @see <a href="http://commons.apache.org/proper/commons-daemon/jsvc.html">jsvc</a>
 * @see <a href="http://commons.apache.org/proper/commons-daemon/procrun.html">jsvc</a>
 *
 * @author circlespainter
 */
public class DaemonCapsule extends Capsule {

	private static final String CONF_FILE = "WindowsServiceCmdline";

	//<editor-fold defaultstate="collapsed" desc="Configuration">
	// Common
	private static final Map.Entry<String, String> ATTR_START_CLASS = ATTRIBUTE("Daemon-Start-Class", T_STRING(), null, true, "Class containing the start method (default: app's main)"); // TODO Test
	private static final Map.Entry<String, String> ATTR_START_METHOD = ATTRIBUTE("Daemon-Start-Method", T_STRING(), null, true, "Static 'String[] -> void' service start method short name run as the specified, if any (default: app's main)"); // TODO Test non-default
	private static final Map.Entry<String, String> ATTR_STOP_CLASS = ATTRIBUTE("Daemon-Stop-Class", T_STRING(), null, true, "Class containing the stop method, if any (default: none)"); // TODO Test
	private static final Map.Entry<String, String> ATTR_STOP_METHOD = ATTRIBUTE("Daemon-Stop-Method", T_STRING(), null, true, "Static 'String[] -> void' service stop method short name run as the specified, if any (default: none)"); // TODO Test
	private static final String PROP_USER = "capsule.daemon.user"; // TODO Test on windows
	private static final Map.Entry<String, String> ATTR_USER = ATTRIBUTE("Daemon-User", T_STRING(), null, true, "The username under which the service will run");
	private static final String PROP_CWD = "capsule.daemon.cwd";
	private static final Map.Entry<String, String> ATTR_CWD = ATTRIBUTE("Daemon-Cwd", T_STRING(), null, true, "Working dir (default: / on Unix)");
	private static final String PROP_STDOUT_FILE = "capsule.daemon.stdoutFile";
	private static final Map.Entry<String, String> ATTR_STDOUT_FILE = ATTRIBUTE("Daemon-Stdout-File", T_STRING(), null, true, "stdout (default: /dev/null on Unix, <logpath>/service-stdout.YEAR-MONTH-DAY.log on Windows)");
	private static final String PROP_STDERR_FILE = "capsule.daemon.stderrFile";
	private static final Map.Entry<String, String> ATTR_STDERR_FILE = ATTRIBUTE("Daemon-Stderr-File", T_STRING(), null, true, "stderr (default: /dev/null on Unix, <logpath>/service-stderr.YEAR-MONTH-DAY.log on Windows))");
	private static final String PROP_PID_FILE = "capsule.daemon.pidFile";
	private static final Map.Entry<String, String> ATTR_PID_FILE = ATTRIBUTE("Daemon-PID-File", T_STRING(), null, true, "PID file (default: /var/run/<appid>.pid on Unix, <logpath>/<appid>.pid on Windows)");

	// Windows only
	private static final String PROP_PASSWORD = "capsule.daemon.password"; // TODO Test
	private static final Map.Entry<String, String> ATTR_PASSWORD = ATTRIBUTE("Daemon-Password", T_STRING(), null, true, "The password of the user under which the service will run (default: none, Windows only)"); // TODO Test
	private static final String PROP_JAVA_EXEC_USER = "capsule.daemon.javaExecUser"; // TODO Test
	private static final Map.Entry<String, String> ATTR_JAVA_EXEC_USER = ATTRIBUTE("Daemon-Java-Exec-User", T_STRING(), null, true, "The password of the user that will execute the final Java process (default: none, Windows only)"); // TODO Test
	private static final String PROP_JAVA_EXEC_PASSWORD = "capsule.daemon.javaExecPassword"; // TODO Test
	private static final Map.Entry<String, String> ATTR_JAVA_EXEC_PASSWORD = ATTRIBUTE("Daemon-Java-Exec-Password", T_STRING(), null, true, "The password of the user that will execute the final Java process (default: none, Windows only)"); // TODO Test
	private static final String PROP_SERVICE_NAME = "capsule.daemon.serviceName"; // TODO Test
	private static final Map.Entry<String, String> ATTR_SERVICE_NAME = ATTRIBUTE("Daemon-Service-Name", T_STRING(), null, true, "The service internal name (default: app ID, Windows only)"); // TODO Test
	private static final String PROP_DISPLAY_NAME = "capsule.daemon.displayName"; // TODO Test
	private static final Map.Entry<String, String> ATTR_DISPLAY_NAME = ATTRIBUTE("Daemon-Display-Name", T_STRING(), null, true, "The service display name (default: app ID, Windows only)"); // TODO Test
	private static final String PROP_DESCRIPTION = "capsule.daemon.description"; // TODO Test
	private static final Map.Entry<String, String> ATTR_DESCRIPTION = ATTRIBUTE("Daemon-Description", T_STRING(), null, true, "The service description (default: app ID, Windows only)"); // TODO Test
	private static final String PROP_STARTUP = "capsule.daemon.startup"; // TODO Test
	private static final Map.Entry<String, String> ATTR_STARTUP = ATTRIBUTE("Daemon-Startup", T_STRING(), null, true, "The service startup mode, either 'auto' or 'manual' (default: manual, Windows only)"); // TODO Test
	private static final String PROP_TYPE = "capsule.daemon.type"; // TODO Test
	private static final Map.Entry<String, String> ATTR_TYPE = ATTRIBUTE("Daemon-Type", T_STRING(), null, true, "The service type, it can be 'interactive' (default: none, Windows only)"); // TODO Test
	private static final String PROP_DEPENDS_ON = "capsule.daemon.dependsOn"; // TODO Test
	private static final Map.Entry<String, List<String>> ATTR_DEPENDS_ON = ATTRIBUTE("Daemon-Depends-On", T_LIST(T_STRING()), null, true, "The service dependencies, as a list (default: none, Windows only)"); // TODO Test
	private static final String PROP_STOP_PARAMS = "capsule.daemon.stopParams"; // TODO Test
	private static final Map.Entry<String, List<String>> ATTR_STOP_PARAMS = ATTRIBUTE("Daemon-Stop-Params", T_LIST(T_STRING()), null, true, "The service stop parameters (default: none, Windows only)"); // TODO Test
	private static final String PROP_STOP_TIMEOUT = "capsule.daemon.stopTimeout"; // TODO Test
	private static final Map.Entry<String, Long> ATTR_STOP_TIMEOUT = ATTRIBUTE("Daemon-Stop-Timeout", T_LONG(), null, true, "Service stop timeout in seconds (default: none, Windows only)"); // TODO Test
	private static final String PROP_LOG_PATH = "capsule.daemon.logPath"; // TODO Test
	private static final Map.Entry<String, String> ATTR_LOG_PATH = ATTRIBUTE("Daemon-Log-Path", T_STRING(), null, true, "The log path (default: %SystemRoot%\\System32\\LogFiles\\Apache, Windows only)"); // TODO Test
	private static final String PROP_LOG_PREFIX = "capsule.daemon.logPrefix"; // TODO Test
	private static final Map.Entry<String, String> ATTR_LOG_PREFIX = ATTRIBUTE("Daemon-Log-Prefix", T_STRING(), null, true, "The log prefix (default: app ID, Windows only)"); // TODO Test
	private static final String PROP_LOG_LEVEL = "capsule.daemon.logLevel"; // TODO Test
	private static final Map.Entry<String, String> ATTR_LOG_LEVEL = ATTRIBUTE("Daemon-Log-Level", T_STRING(), null, true, "The log level between 'error', 'info', 'warn' and 'debug' (default: info, Windows only)"); // TODO Test

	// Unix only
	private static final String PROP_CHECK_ONLY = "capsule.daemon.checkOnly"; // TODO Test
	private static final String PROP_DEBUG = "capsule.daemon.debug";
	private static final String PROP_VERBOSE = "capsule.daemon.verbose"; // TODO Test

	private static final Map.Entry<String, String> ATTR_INIT_CLASS = ATTRIBUTE("Init-Class", T_STRING(), null, true, "Class containing the init method (default: none, Unix only)");  // TODO Test
	private static final Map.Entry<String, String> ATTR_INIT_METHOD = ATTRIBUTE("Init-Method", T_STRING(), null, true, "Static 'String[] -> String[]' service initialization method short name run as 'root'; the return value will be passed to the 'Start' method (default: none, Unix only)");  // TODO Test
	private static final Map.Entry<String, String> ATTR_DESTROY_CLASS = ATTRIBUTE("Destroy-Class", T_STRING(), null, true, "Class containing the destroy method (default: none, Unix only)"); // TODO Test
	private static final Map.Entry<String, String> ATTR_DESTROY_METHOD = ATTRIBUTE("Destroy-Method", T_STRING(), null, true, "Static service cleanup method short name run as 'root' (default: none, Unix only)"); // TODO Test
	private static final String PROP_NO_DETACH = "capsule.daemon.noDetach"; // TODO Test
	private static final Map.Entry<String, Boolean> ATTR_NO_DETACH = ATTRIBUTE("No-Detach", T_BOOL(), false, true, "Don't detach from parent process (default: false, Unix only)"); // TODO Test
	private static final String PROP_KEEP_STDIN = "capsule.daemon.keepStdin"; // TODO Test
	private static final Map.Entry<String, Boolean> ATTR_KEEP_STDIN = ATTRIBUTE("Keep-Stdin", T_BOOL(), false, true, "Don't redirect stdin to /dev/null (default: false, Unix only)"); // TODO Test
	private static final String PROP_WAIT_SECS = "capsule.daemon.waitSecs"; // TODO Test
	private static final Map.Entry<String, Long> ATTR_WAIT_SECS = ATTRIBUTE("Wait-Secs", T_LONG(), null, true, "Wait seconds for service, must be multiple of 10 (default: 10 secs, Unix only)"); // TODO Test
	//</editor-fold>

	private static Path hostAbsoluteOwnJarFile;
	private static Path svcExec;
	private Map<String, String> env;

	public DaemonCapsule(Capsule pred) {
		super(pred);
	}

	@Override
	protected Path getJavaExecutable() {
		if (svcExec == null)
			svcExec = setupBinDir().resolve(platformExecPath()).toAbsolutePath().normalize();
		return svcExec;
	}

	@Override
	protected Map<String, String> buildEnvironmentVariables(Map<String, String> env) {
		this.env = env;
		return super.buildEnvironmentVariables(env);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> T attribute(Map.Entry<String, T> attr) {
		if (ATTR_APP_CLASS_PATH == attr && isWrapperCapsule()) {
			final List<Object> cp = new ArrayList<>(super.attribute(ATTR_APP_CLASS_PATH));
			cp.add(findOwnJarFile().toAbsolutePath().normalize());
			return (T) cp;
		}
		return super.attribute(attr);
	}

	@Override
	protected final ProcessBuilder prelaunch(List<String> jvmArgs, List<String> args) {
		final ProcessBuilder pb = super.prelaunch(jvmArgs, args);
		final List<String> svcCmd;
		try {
			svcCmd = toSvc(pb.command());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return new ProcessBuilder(svcCmd);
	}

	@Override
	protected Process postlaunch(Process child) {
		return null; // Don't wait for the child: the management of the service application is delegated to 'jsvc'/'procrun'
	}

	//<editor-fold defaultstate="collapsed" desc="Utils">
	private Path setupBinDir() {
		try {
			final Path libdir = findOwnJarFile().toAbsolutePath().getParent().resolve("bin");
			final String[] ress =  new String[] {
				"jsvc/linux64-brew/jsvc",
				"jsvc/macosx-yosemite-brew/jsvc",
				"procrun/prunsrv.exe",
			};
			log(LOG_VERBOSE, "Copying daemon native helpers " + Arrays.toString(ress) + " in " + libdir.toAbsolutePath().normalize().toString());
			if (Files.exists(libdir))
				delete(libdir);
			addTempFile(Files.createDirectory(libdir));

			for (final String filename : ress)
				copy(filename, "bin", libdir);

			return libdir;
		} catch (IOException e) {
			throw new RuntimeException("Could not extract jsvc/procrun executables", e);
		}
	}

	private static Path copy(String filename, String resourceDir, Path targetDir, OpenOption... opts) throws IOException {
		try (final InputStream in = DaemonCapsule.class.getClassLoader().getResourceAsStream(resourceDir + '/' + filename)) {
			final Path f = targetDir.resolve(filename);
			Files.createDirectories(f.getParent());
			try (final OutputStream out = Files.newOutputStream(f, opts)) {
				copy(in, out);
				final Path ret = targetDir.resolve(filename);
				//noinspection ResultOfMethodCallIgnored
				ret.toFile().setExecutable(true);
				log(LOG_VERBOSE, "Successfully copied resource " + resourceDir + "/" + filename + " to " + targetDir.toAbsolutePath().normalize().toString());
				return ret;
			}
		}
	}

	private List<String> toSvc(List<String> cmd) throws IOException {
		if (isWindows())
			return setupWindowsCmd(cmd);
		else
			return setupUnixCmd(cmd);
	}

	private List<String> setupWindowsCmd(List<String> cmd) throws IOException {
		String svcName = getPropertyOrAttributeString(PROP_SERVICE_NAME, ATTR_SERVICE_NAME);
		if (svcName == null)
			svcName = getAppId();

		final List<String> installCmd = new ArrayList<>();

		installCmd.add(svcExec.toString());
		installCmd.add("install");
		installCmd.add(svcName);

		final List<String> jvmOpts = new ArrayList<>();
		final List<String> appOpts = new ArrayList<>();

		// TODO Not nicest but redefining ATTR_APP_CLASS seems to break a lot of stuff
		final String appClass = parseWindows(cmd, installCmd, jvmOpts, appOpts);

		int i = installCmd.size();

		installCmd.add(i++, "--JavaHome");
		installCmd.add(i++, getJavaHome().toAbsolutePath().normalize().toString());

		// Add attrs
		installCmd.add(i++, "--Description");
		final String desc = getPropertyOrAttributeString(PROP_DESCRIPTION, ATTR_DESCRIPTION);
		installCmd.add(i++, desc != null ? desc : getAppId());

		installCmd.add(i++, "--DisplayName");
		final String dName = getPropertyOrAttributeString(PROP_DISPLAY_NAME, ATTR_DISPLAY_NAME);
		installCmd.add(i++, dName != null ? dName : getAppId());

		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_STARTUP, ATTR_STARTUP, "--Startup", i);
		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_TYPE, ATTR_TYPE, "--Type", i);

		// TODO mangle http://commons.apache.org/proper/commons-daemon/procrun.html
		final List<String> dependsOn = getPropertyOrAttributeStringList(PROP_DEPENDS_ON, ATTR_DEPENDS_ON);
		if (dependsOn != null && !dependsOn.isEmpty()) {
			installCmd.add(i++, "++DependsOn");
			installCmd.add(i++, join(dependsOn, ";"));
		}

		// Add env
		// TODO mangle http://commons.apache.org/proper/commons-daemon/procrun.html
		if (env != null && !env.isEmpty()) {
			final ArrayList<String> envL = new ArrayList<>();
			for (final String e : env.keySet())
				envL.add(e + "=" + env.get(e));
			if (!envL.isEmpty()) {
				installCmd.add(i++, "++Environment");
				installCmd.add(i++, join(envL, ";"));
			}
		}

		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_JAVA_EXEC_USER, ATTR_JAVA_EXEC_USER, "--User", i);
		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_JAVA_EXEC_PASSWORD, ATTR_JAVA_EXEC_PASSWORD, "--Password", i);
		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_USER, ATTR_USER, "--ServiceUser", i);
		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_PASSWORD, ATTR_PASSWORD, "--ServicePassword", i);

		installCmd.add(i++, "--StartMode");
		installCmd.add(i++, "Java");

		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_CWD, ATTR_CWD, "--StartPath", i);

		// Not using DaemonAdapter, not needed for Windows

		installCmd.add(i++, "--StartClass");
		final String startC = getAttribute(ATTR_START_CLASS);
		installCmd.add(i++, (startC != null ? startC : appClass));

		final String startM = getAttribute(ATTR_START_METHOD);
		if (startM != null) {
			installCmd.add(i++, "--StartMethod");
			installCmd.add(i++, startM);
		}

		// TODO mangle http://commons.apache.org/proper/commons-daemon/procrun.html
		if (!appOpts.isEmpty()) {
			installCmd.add(i++, "++StartParams");
			installCmd.add(i++, join(appOpts, ";"));
		}

		installCmd.add(i++, "--StopMode");
		installCmd.add(i++, "Java");

		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_CWD, ATTR_CWD, "--StopPath", i);

		final String stopC = getAttribute(ATTR_STOP_CLASS);
		if (stopC != null) {
			installCmd.add(i++, "--StopClass");
			installCmd.add(i++, stopC);
		}

		final String stopM = getAttribute(ATTR_STOP_METHOD);
		if (stopM != null) {
			installCmd.add(i++, "--StopMethod");
			installCmd.add(i++, stopM);
		}

		// TODO mangle http://commons.apache.org/proper/commons-daemon/procrun.html
		final List<String> stopParams = getPropertyOrAttributeStringList(PROP_STOP_PARAMS, ATTR_STOP_PARAMS);
		if (stopParams != null && !stopParams.isEmpty()) {
			installCmd.add(i++, "++StopParams");
			installCmd.add(i++, join(stopParams, ";"));
		}

		final Long stopTimeout = getPropertyOrAttributeLong(PROP_STOP_TIMEOUT, ATTR_STOP_TIMEOUT);
		if (stopTimeout != null) {
			installCmd.add(i++, "++StopTimeout");
			installCmd.add(i++, stopTimeout.toString());
		}

		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_LOG_PATH, ATTR_LOG_PATH, "--LogPath", i);

		installCmd.add(i++, "--LogPrefix");
		final String logPrefix = getPropertyOrAttributeString(PROP_LOG_PREFIX, ATTR_LOG_PREFIX);
		installCmd.add(i++, logPrefix != null ? logPrefix : getAppId());

		i = addPropertyOrAttributeStringAsOption(installCmd, PROP_LOG_LEVEL, ATTR_LOG_LEVEL, "--LogLevel", i);

		installCmd.add(i++, "--StdOutput");
		final String stdout = getPropertyOrAttributeString(PROP_STDOUT_FILE, ATTR_STDOUT_FILE);
		installCmd.add(i++, stdout != null ? stdout : "auto");

		installCmd.add(i++, "--StdError");
		final String stderr = getPropertyOrAttributeString(PROP_STDERR_FILE, ATTR_STDERR_FILE);
		installCmd.add(i++, stderr != null ? stderr : "auto");

		installCmd.add(i++, "--PidFile");
		final String pid = getPropertyOrAttributeString(PROP_PID_FILE, ATTR_PID_FILE);
		installCmd.add(i++, pid != null ? pid : getAppId() + ".pid");

		// TODO mangle http://commons.apache.org/proper/commons-daemon/procrun.html
		installCmd.add(i++, "++JvmOptions");
		installCmd.add(i, join(jvmOpts, ";"));

		final String installCmdline = join(installCmd, " ");
		if (isReinstallNeeded(installCmdline)) {
			// Write new install cmdline
			dump(installCmdline, getCmdlineFile());

			// Remove old service
			try {
				final ProcessBuilder pb = new ProcessBuilder().command(svcExec.toString(), "delete", svcName);
				final Process p = pb.start();
				if (p.waitFor() != 0)
					log(LOG_VERBOSE, "Windows: couldn't delete service " + svcName + ".\n\tstderr:\n\t\t" + slurp(p.getErrorStream()) + "\n\tstdout:\n\t\t" + slurp(p.getInputStream()));
				else
					log(LOG_VERBOSE, "Windows: service " + svcName + " successfully deleted");
			} catch (InterruptedException | IOException ignored) {
				log(LOG_VERBOSE, "Windows: couldn't delete service " + svcName + ", exception message: " + ignored.getMessage());
				// Try proceeding anyway
			}

			// Re-install
			try {
				log(LOG_VERBOSE, "Windows: installing service " + svcName + " with command: " + installCmd.toString());
				final Process p = new ProcessBuilder(installCmd).start();
				if (p.waitFor() != 0)
					log(LOG_VERBOSE, "Windows: couldn't install install " + svcName + ".\n\tstderr:\n\t\t" + slurp(p.getErrorStream()) + "\n\tstdout:\n\t\t" + slurp(p.getInputStream()));
				else
					log(LOG_VERBOSE, "Windows: service " + svcName + " successfully installed");
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException(e);
			}
		}

		// Return command for service start
		final List<String> ret = new ArrayList<>();
		ret.add(svcExec.toString());
		ret.add("start");
		ret.add(svcName);
		return ret;
	}

	private boolean isReinstallNeeded(String cmdLine) throws IOException {
		// Check if the conf file exists
		if (!Files.exists(getCmdlineFile())) {
			log(LOG_VERBOSE, "Service install cmdline file " + getCmdlineFile() + " is not present");
			return true;
		}

		// Check if the conf content has changed
		if (!new String(Files.readAllBytes(getCmdlineFile()), Charset.defaultCharset()).equals(cmdLine)) {
			log(LOG_VERBOSE, "Service install cmdline file content " + getCmdlineFile() + " has changed");
			return true;
		}

		// Check if the application is newer
		try {
			FileTime jarTime = Files.getLastModifiedTime(getJarFile());
			if (isWrapperCapsule()) {
				final FileTime wrapperTime = Files.getLastModifiedTime(findOwnJarFile());
				if (wrapperTime.compareTo(jarTime) > 0)
					jarTime = wrapperTime;
			}

			final FileTime confTime = Files.getLastModifiedTime(getCmdlineFile());

			final boolean buildNeeded = confTime.compareTo(jarTime) < 0;
			if (buildNeeded)
				log(LOG_VERBOSE, "Application " + getJarFile() + " has changed");
			return buildNeeded;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Path getDaemonDir() throws IOException {
		final Path ret = appDir().resolve("daemon");
		if (!Files.exists(ret))
			Files.createDirectories(ret);
		return ret;
	}

	private Path getCmdlineFile() throws IOException {
		return getDaemonDir().resolve(CONF_FILE);
	}

	private int addPropertyOrAttributeStringAsOption(List<String> outCmd, String prop, Map.Entry<String, String> attr, String opt, int pos) {
		final String v = getPropertyOrAttributeString(prop, attr);
		if (v != null) {
			outCmd.add(pos++, opt);
			outCmd.add(pos++, v);
		}
		return pos;
	}

	private int addAttributeStringAsProperty(List<String> outCmd, Map.Entry<String, String> inAttr, String outPropKey, int pos) {
		final String v = getAttribute(inAttr);
		if (v != null)
			outCmd.add(pos++, "-D" + outPropKey + "=" + v);
		return pos;
	}

	private String parseWindows(List<String> cmds, List<String> outCmdOpts, List<String> outJvmOpts, List<String> outAppOpts) {
		final List<String> otherJvmOpts = new ArrayList<>();
		boolean addToCmdOpts = false;
		for (final String c : cmds.subList(1, cmds.size())) { // Skip actual command
			if (addToCmdOpts) {
				addToCmdOpts = false;
				outCmdOpts.add(c);
			}
			else if ("-cp".equals(c) || "-classpath".equals(c)) {
				outCmdOpts.add("--Classpath");
				addToCmdOpts = true;
			}
			else if ("-Xmx".equals(c))
				outJvmOpts.add("--JvmMx");
			else if ("-Xms".equals(c))
				outJvmOpts.add("--JvmMs");
			else if ("-Xss".equals(c))
				outJvmOpts.add("--JvmSs");
			else if (c.startsWith("-Djava.library.path=")) {
				outCmdOpts.add("--LibraryPath");
				outCmdOpts.add(c.substring("-Djava.library.path=".length()));
			}
			else if (c.startsWith("-D") || c.startsWith("-X")
				// TODO check if they are supported by procrun
				|| "-server".equals(c) || "-client".equals(c) || "-d32".equals(c) || "-d64".equals(c)
				|| "-?".equals(c) || "-help".equals(c) || "-showversion".equals(c)
				|| "-esa".equals(c) || "-dsa".equals(c) || "-enablesystemassertions".equals(c) || "-disablesystemassertions".equals(c)
				|| c.startsWith("-agentlib") || c.startsWith("-agentpath") || c.startsWith("-javaagent")
				|| c.startsWith("-ea") || c.startsWith("-da") || c.startsWith("-enableassertions") || c.startsWith("-disableassertions")
				|| c.startsWith("-version") || c.startsWith("-verbose:") || c.startsWith("-splash:"))
				otherJvmOpts.add(c);
			else
				outAppOpts.add(c);
		}
		// TODO mangle http://commons.apache.org/proper/commons-daemon/procrun.html
		outJvmOpts.add(join(otherJvmOpts, ";"));

		return outAppOpts.remove(outAppOpts.indexOf(getAttribute(ATTR_APP_CLASS)));
	}

	private List<String> setupUnixCmd(List<String> cmd) {
		final List<String> ret = new ArrayList<>(cmd);

		int i = 1;
		ret.add(i++, "-java-home");
		Path javaHome = getJavaHome().toAbsolutePath().normalize();
		if (isMac() && javaHome.toString().endsWith("/Home/jre"))
			javaHome = javaHome.getParent();
		ret.add(i++, javaHome.toString());

		i = addPropertyOrAttributeStringAsOption(ret, PROP_USER, ATTR_USER, "-user", i);

		if (getPropertyOrAttributeBool(PROP_KEEP_STDIN, ATTR_KEEP_STDIN))
			ret.add(i++, "-keepstdin");

		if (getPropertyOrAttributeBool(PROP_NO_DETACH, ATTR_NO_DETACH))
			ret.add(i++, "-nodetach");

		final String checkOnly = System.getProperty(PROP_CHECK_ONLY);
		if (checkOnly != null && !"false".equals(checkOnly))
			ret.add(i++, "-check");

		final String debug = System.getProperty(PROP_DEBUG);
		if (debug != null && !"false".equals(debug))
			ret.add(i++, "-debug");

		final String verbose = System.getProperty(PROP_VERBOSE);
		if (verbose != null) {
			if (verbose.length() == 0)
				ret.add(i++, "-verbose");
			else
				ret.add(i++, "-verbose:" + verbose);
		}

		i = addPropertyOrAttributeStringAsOption(ret, PROP_CWD, ATTR_CWD, "-cwd", i);
		i = addPropertyOrAttributeStringAsOption(ret, PROP_STDOUT_FILE, ATTR_STDOUT_FILE, "-outfile", i);
		i = addPropertyOrAttributeStringAsOption(ret, PROP_STDERR_FILE, ATTR_STDERR_FILE, "-errfile", i);

		ret.add(i++, "-pidfile");
		final String pid = getPropertyOrAttributeString(PROP_PID_FILE, ATTR_PID_FILE);
		ret.add(i++, pid != null ? pid : "/var/run/" + getAppId() + ".pid");

		final Long wait = getPropertyOrAttributeLong(PROP_WAIT_SECS, ATTR_WAIT_SECS);
		if (wait != null) {
			ret.add(i++, "-wait");
			ret.add(i++, wait.toString());
		}

		i = addAttributeStringAsProperty(ret, ATTR_INIT_CLASS, DaemonAdapter.PROP_INIT_CLASS, i);
		i = addAttributeStringAsProperty(ret, ATTR_INIT_METHOD, DaemonAdapter.PROP_INIT_METHOD, i);

		// TODO Not nicest but redefining ATTR_APP_CLASS seems to break a lot of stuff
		final String startC = getAttribute(ATTR_START_CLASS);
		final int appClassIdx = ret.indexOf(getAttribute(ATTR_APP_CLASS));
		final String appClass = ret.remove(appClassIdx);
		ret.add(appClassIdx, DaemonAdapter.class.getName());
		ret.add(i++, "-D" + DaemonAdapter.PROP_START_CLASS + "=" + (startC != null ? startC : appClass));

		final String startM = getAttribute(ATTR_START_METHOD);
		ret.add(i++, "-D" + DaemonAdapter.PROP_START_METHOD + "=" + (startM != null ? startM : "main"));
		i = addAttributeStringAsProperty(ret, ATTR_STOP_CLASS, DaemonAdapter.PROP_STOP_METHOD, i);
		i = addAttributeStringAsProperty(ret, ATTR_DESTROY_CLASS, DaemonAdapter.PROP_DESTROY_CLASS, i);
		addAttributeStringAsProperty(ret, ATTR_DESTROY_METHOD, DaemonAdapter.PROP_DESTROY_METHOD, i);

		return ret;
	}

	private String getPropertyOrAttributeString(String propName, Map.Entry<String, String> attr) {
		final String propValue = System.getProperty(propName);
		if (propValue == null)
			return getAttribute(attr);
		return propValue;
	}

	private List<String> getPropertyOrAttributeStringList(String propName, Map.Entry<String, List<String>> attr) {
		final String propValue = System.getProperty(propName);
		if (propValue == null)
			return getAttribute(attr);
		return Arrays.asList(propValue.split(";"));
	}


	private Boolean getPropertyOrAttributeBool(String propName, Map.Entry<String, Boolean> attr) {
		final String propValue = System.getProperty(propName);
		if (propValue == null)
			return getAttribute(attr);
		try {
			return Boolean.parseBoolean(propValue);
		} catch (Throwable t) {
			return getAttribute(attr);
		}
	}

	private Long getPropertyOrAttributeLong(String propName, Map.Entry<String, Long> attr) {
		final String propValue = System.getProperty(propName);
		if (propValue == null)
			return getAttribute(attr);
		try {
			return Long.parseLong(propValue);
		} catch (Throwable t) {
			return getAttribute(attr);
		}
	}

	private boolean isLinux64() {
		return "linux".equals(getPlatform()) && System.getProperty("os.arch").contains("64");
	}

	private Path platformExecPath() {
		if (isMac())
			return Paths.get("jsvc", "macosx-yosemite-brew", "jsvc");
		if (isWindows())
			return Paths.get("procrun", "prunsrv.exe");
		if (isLinux64()) {
			return Paths.get("jsvc", "linux64-brew", "jsvc");
		}
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new ProcessBuilder("which", "jsvc").start().getInputStream(), Charset.defaultCharset()))) {
			return Paths.get(reader.readLine());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Path findOwnJarFile() {
		if (hostAbsoluteOwnJarFile == null) {
			final URL url = DaemonCapsule.class.getClassLoader().getResource(DaemonCapsule.class.getName().replace('.', '/') + ".class");
			if (url != null) {
				if (!"jar".equals(url.getProtocol()))
					throw new IllegalStateException("The Capsule class must be in a JAR file, but was loaded from: " + url);
				final String path = url.getPath();
				if (path == null) //  || !path.startsWith("file:")
					throw new IllegalStateException("The Capsule class must be in a local JAR file, but was loaded from: " + url);

				try {
					final URI jarUri = new URI(path.substring(0, path.indexOf('!')));
					hostAbsoluteOwnJarFile = Paths.get(jarUri).toAbsolutePath().normalize();
				} catch (URISyntaxException e) {
					throw new AssertionError(e);
				}
			} else
				throw new RuntimeException("Can't locate capsule's own class");
		}
		return hostAbsoluteOwnJarFile;
	}

	private static String join(List<String> values, String sep) {
		final ArrayList<String> vals = values != null ? new ArrayList<>(values) : new ArrayList<String>();
		final String v0 = vals.size() > 0 ? vals.remove(0) : "";
		final StringBuilder sb = new StringBuilder(v0);
		for (String v : vals)
			sb.append(sep).append(v);
		return sb.toString();
	}

	private static String slurp(InputStream in) throws IOException {
		final StringBuilder out = new StringBuilder();
		final byte[] b = new byte[4096];
		int n;
		while ((n = in.read(b)) != -1)
			out.append(new String(b, 0, n));

		return out.toString();
	}

	private static void dump(String content, Path loc) throws IOException {
		try (final PrintWriter out = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(loc), Charset.defaultCharset()))) {
			out.print(content);
		}
	}
	//</editor-fold>
}
