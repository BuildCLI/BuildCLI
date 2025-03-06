package dev.buildcli.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static dev.buildcli.hooks.HookPhase.AFTER;
import static dev.buildcli.hooks.HookPhase.BEFORE;

public class HookExecutor {
    private static final Logger log = LoggerFactory.getLogger(HookExecutor.class);
    private final List<Hook> hooks;

    public HookExecutor(List<Hook> hooks) {
        this.hooks = hooks;
    }

    public void executeHook(String[] args, CommandLine commandLine) {
        List<String[]> orderedHooks = orderHooks(args);
        List<Integer> exitCodes = new ArrayList<>();
        for (String[] command : orderedHooks) {
            log.info("\nExecuting command: {}", command);
            exitCodes.add(commandLine.execute(command));

        }

        if (exitCodes.stream().allMatch(c -> c == 0))
            System.exit(0);

        System.exit(1);
    }

    private List<String[]> orderHooks(String[] args){
        List<String[]> orderedHooks = new ArrayList<>();
        String command = String.join(" ",args);
        orderedHooks.add(args);
        for(Hook hook: hooks){
            String[] s = hook.hookCommand().replace("[]", "").split(" ");
            if (hook.command().equals(command) && hook.phase() == BEFORE){
                orderedHooks.addFirst(s);
            } else if (hook.command().equals(command) && hook.phase() == AFTER){
                orderedHooks.addLast(s);
            }
        }
        return orderedHooks;
    }
}