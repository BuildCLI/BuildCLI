package dev.buildcli.hooks;

import dev.buildcli.hooks.phase.HookPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;

public class HookManager {
    private static final Logger log = LoggerFactory.getLogger(HookManager.class);
    private final Set<Hook> hooks = new HashSet<>();
    private final HookLoader hookLoader;
    private final HookValidator hookValidator;
    private final HookExecutor hookExecutor;

    public HookManager(CommandLine command) {
        this.hookLoader = new HookLoader();
        this.hookValidator = new HookValidator(command);
        this.hookExecutor = new HookExecutor(hooks);
        this.hooks.addAll(hookLoader.loadHooks());
    }

    public void registerHook(Hook hook) {

        if (!hookValidator.isValidCommand(hook.command())) {
            log.error("Invalid command: {}", hook.command());
            throw new IllegalArgumentException("Unknown command: " + hook.command());
        }
        if (!hookValidator.isValidPhase(String.valueOf(hook.phase()))){
            log.error("Invalid phase: {}", hook.phase());
            throw new IllegalArgumentException("Unknown phase: " + hook.phase());
        }
        if (hookValidator.isHookAlreadyRegistered(hooks, hook)) {
            log.error("Hook already registered: {}", hook);
            throw new IllegalArgumentException("Hook already registered: " + hook);
        }

        hooks.add(hook);
        hookLoader.saveHooks(hooks);

        log.info("Hook registered : {}", hook);
    }

    public void removeHook(String commandName, HookPhase phase) {
        log.info("Removing hook for command: {}, phase: {}", commandName, phase);
        hooks.removeIf(h -> h.command().equals(commandName) && h.phase() == phase);
        hookLoader.saveHooks(hooks);
    }

    public void executeHook(String[] args, CommandLine commandLine) {
        hookExecutor.executeHook(args, commandLine);
    }

    public void listHooks() {
        StringBuilder myHooks = new StringBuilder(hooks.toString().replace("]", "]\n").replace("\n, ", "\n"));
        if (myHooks.length() > 1) {
            int bracketAtEnd = 2;
            myHooks.deleteCharAt(0);
            myHooks.setLength(myHooks.length() - bracketAtEnd);
        }
        log.info("Hooks configured: \n{}", myHooks);
    }
}