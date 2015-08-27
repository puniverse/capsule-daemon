/*
 * Capsule
 * Copyright (c) 2015, Parallel Universe Software Co. and Contributors. All rights reserved.
 *
 * This program and the accompanying materials are licensed under the terms
 * of the Eclipse Public License v1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
import co.paralleluniverse.capsule.daemon.DaemonAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A caplet that will use jsvc (needs to be installed) to launch the application as an Unix daemon. Several configuration options are provided.
 *
 * @see <a href="http://commons.apache.org/proper/commons-daemon/jsvc.html">jsvc</a>
 * @author circlespainter
 */
public class DaemonCapsule extends Capsule {

	private static final Map.Entry<String, String> ATTR_INIT_METHOD = ATTRIBUTE("Init", T_STRING(), null, true, "Static service initialization method run as root (default: none)");
	private static final Map.Entry<String, String> ATTR_START_METHOD = ATTRIBUTE("Start", T_STRING(), null, true, "Static service initialization method run as the specified user, if any (default: app's main)");
	private static final Map.Entry<String, String> ATTR_STOP_METHOD = ATTRIBUTE("Stop", T_STRING(), null, true, "Static service initialization method run as the specified user, if any (default: none)");
	private static final Map.Entry<String, String> ATTR_DESTROY_METHOD = ATTRIBUTE("Destroy", T_STRING(), null, true, "Static service cleanup method run as root (default: none)");

	private static final String PROP_USER = "capsule.daemon.user";
	private static final Map.Entry<String, String> ATTR_USER = ATTRIBUTE("User", T_STRING(), null, true, "Static service initialization method, run as root (default: none)");
	private static final String PROP_NO_DETACH = "capsule.daemon.noDetach";
	private static final Map.Entry<String, Boolean> ATTR_NO_DETACH = ATTRIBUTE("No-Detach", T_BOOL(), false, true, "Don't detach from parent process (default: false)");
	private static final String PROP_KEEP_STDIN = "capsule.daemon.keepStdin";
	private static final Map.Entry<String, Boolean> ATTR_KEEP_STDIN = ATTRIBUTE("Keep-Stdin", T_BOOL(), false, true, "Don't redirect stdin to /dev/null (default: false)");
	private static final String PROP_CWD = "capsule.daemon.cwd";
	private static final Map.Entry<String, String> ATTR_CWD = ATTRIBUTE("Cwd", T_STRING(), null, true, "Working dir (default: /)");
	private static final String PROP_STDOUT_FILE = "capsule.daemon.stdoutFile";
	private static final Map.Entry<String, String> ATTR_STDOUT_FILE = ATTRIBUTE("Stdout-File", T_STRING(), null, true, "stdout (default: /dev/null)");
	private static final String PROP_STDERR_FILE = "capsule.daemon.stderrFile";
	private static final Map.Entry<String, String> ATTR_STDERR_FILE = ATTRIBUTE("Stderr-File", T_STRING(), null, true, "stderr (default: /dev/null)");
	private static final String PROP_PID_FILE = "capsule.daemon.pidFile";
	private static final Map.Entry<String, String> ATTR_PID_FILE = ATTRIBUTE("PID-File", T_STRING(), null, true, "PID file (default: /var/run/<appid>.pid)");
	private static final String PROP_WAIT_SECS = "capsule.daemon.waitSecs";
	private static final Map.Entry<String, Long> ATTR_WAIT_SECS = ATTRIBUTE("Wait-Secs", T_LONG(), null, true, "Wait seconds for service, must be multiple of 10 (default: 10 secs)");

	private static final String PROP_CHECK_ONLY = "capsule.daemon.checkOnly";
	private static final String PROP_DEBUG = "capsule.daemon.debug";
	private static final String PROP_VERBOSE = "capsule.daemon.verbose";

	private static Path hostAbsoluteOwnJarFile;

	public DaemonCapsule(Capsule pred) {
		super(pred);
	}

	@Override
	protected final ProcessBuilder prelaunch(List<String> jvmArgs, List<String> args) {
		final ProcessBuilder pb = super.prelaunch(jvmArgs, args);
		final List<String> jsvcCmd = toJsvc(pb.command());
		return new ProcessBuilder(jsvcCmd);
	}

	@Override
	protected Path getJavaExecutable() {
		try {
			final Process p = new ProcessBuilder("which", "jsvc").start();
			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
				return Paths.get(reader.readLine());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> T attribute(Map.Entry<String, T> attr) {
		if (ATTR_APP_CLASS_PATH == attr) {
			final List<Object> cp = new ArrayList<>(super.attribute(ATTR_APP_CLASS_PATH));
			cp.add(findOwnJarFile().toAbsolutePath().normalize());
			return (T) cp;
		}
		return super.attribute(attr);
	}

	//<editor-fold defaultstate="collapsed" desc="Utils">
	private List<String> toJsvc(List<String> cmd) {
		final ArrayList<String> ret = new ArrayList<>(cmd);

		int i = 1;
		ret.add(i++, "-java-home");
		ret.add(i++, getJavaHome().toAbsolutePath().normalize().toString());

		final String user = getPropertyOrAttributeString(PROP_USER, ATTR_USER);
		if (user != null) {
			ret.add(i++, "-user");
			ret.add(i++, user);
		}

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

		final String cwd = getPropertyOrAttributeString(PROP_CWD, ATTR_CWD);
		if (cwd != null) {
			ret.add(i++, "-cwd");
			ret.add(i++, cwd);
		}

		final String stdout = getPropertyOrAttributeString(PROP_STDOUT_FILE, ATTR_STDOUT_FILE);
		if (stdout != null) {
			ret.add(i++, "-outfile");
			ret.add(i++, stdout);
		}

		final String stderr = getPropertyOrAttributeString(PROP_STDERR_FILE, ATTR_STDERR_FILE);
		if (stderr != null) {
			ret.add(i++, "-errfile");
			ret.add(i++, stderr);
		}

		ret.add(i++, "-pidfile");
		final String pid = getPropertyOrAttributeString(PROP_PID_FILE, ATTR_PID_FILE);
		if (pid != null)
			ret.add(i++, pid);
		else
			ret.add(i++, "/var/run/" + getAppId() + ".pid");

		final Long wait = getPropertyOrAttributeLong(PROP_WAIT_SECS, ATTR_WAIT_SECS);
		if (wait != null) {
			ret.add(i++, "-wait");
			ret.add(i++, wait.toString());
		}

		final String init = getAttribute(ATTR_INIT_METHOD);
		if (init != null)
			ret.add(i++, "-D" + DaemonAdapter.PROP_INIT + "=" + init);

		final String stop = getAttribute(ATTR_STOP_METHOD);
		if (stop != null)
			ret.add(i++, "-D" + DaemonAdapter.PROP_STOP + "=" + stop);

		final String destroy = getAttribute(ATTR_DESTROY_METHOD);
		if (destroy != null)
			ret.add(i++, "-D" + DaemonAdapter.PROP_DESTROY + "=" + destroy);

		final String start = getAttribute(ATTR_START_METHOD);
		final String appClass = ret.remove(ret.size() - 1); // Class
		ret.add(i, "-D" + DaemonAdapter.PROP_START + "=" + (start != null ? start : appClass + ".main"));

		ret.add(DaemonAdapter.class.getName()); // Not nicest but redefining ATTR_APP_CLASS seems to break a lot of stuff

		return ret;
	}

	private String getPropertyOrAttributeString(String propName, Map.Entry<String, String> attr) {
		final String propValue = System.getProperty(propName);
		if (propValue == null)
			return getAttribute(attr);
		return propValue;
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
					hostAbsoluteOwnJarFile = Paths.get(jarUri);
				} catch (URISyntaxException e) {
					throw new AssertionError(e);
				}
			} else
				throw new RuntimeException("Can't locate capsule's own class");
		}
		return hostAbsoluteOwnJarFile;
	}
	//</editor-fold>
}
