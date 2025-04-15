package dev.buildcli.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.buildcli.hooks.phase.HookPhase.*;

public class HookExecutor {
    private static final Logger log = LoggerFactory.getLogger(HookExecutor.class);
    private final Set<Hook> hooks;

    public HookExecutor(Set<Hook> hooks) {
        this.hooks = hooks;
    }

    public void executeHook(String[] args, CommandLine commandLine) {
        List<String[]> orderedHooks = orderHooks(args);
        List<Integer> exitCodes = new ArrayList<>();
        for (String[] command : orderedHooks) {
            if(orderedHooks.size()>1) {
                var commandString = String.join(" ", command);
                log.info("\nExecuting command: {}", commandString);
            }
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