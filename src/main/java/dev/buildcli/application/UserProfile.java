package dev.buildcli.application;

import dev.buildcli.ports.SettingsStore.Scope;

/**
 * What the user told BuildCLI about themselves: a name, a few lines about them, and how they like to be dealt with. It lives on
 * this computer (in the user's own settings, never in a project) and goes into the prompt of every agent, so they can talk to a
 * person rather than to "the user".
 */
public final class UserProfile {
    /** The longest each part is kept, so a profile cannot crowd out the work. */
    static final int MAX_PART = 1500;

    private UserProfile() { }

    /** The paragraph that tells an agent who it is talking to. Never empty: even with no profile it says how to address them. */
    public static String describe(Settings settings) {
        String name = part(settings.get(Settings.PROFILE_NAME), 80);
        String about = part(settings.get(Settings.PROFILE_ABOUT), MAX_PART);
        String style = part(settings.get(Settings.PROFILE_STYLE), MAX_PART);
        StringBuilder sb = new StringBuilder("The person you work for");
        sb.append(name.isEmpty() ? " has not told you their name. Say 'you' to them."
                : " is " + name + ". Call them " + name + " now and then, like a colleague would, and say 'you' otherwise.");
        sb.append(" Never call them 'the user'.");
        if (!about.isEmpty()) {
            sb.append("\nWhat they told you about themselves (use it when it helps; do not recite it): ").append(about);
        }
        if (!style.isEmpty()) {
            sb.append("\nHow they want you to deal with them (follow it): ").append(style);
        }
        return sb.toString();
    }

    /** Stores one part in the user's own settings for all projects. */
    public static void set(Settings settings, String key, String value) {
        settings.set(Scope.GLOBAL, key, value == null ? "" : part(value, MAX_PART));
    }

    private static String part(String text, int max) {
        String t = text == null ? "" : text.strip().replaceAll("[\\p{Cntrl}&&[^\\n]]", " ");
        return t.length() > max ? t.substring(0, max) : t;
    }
}
