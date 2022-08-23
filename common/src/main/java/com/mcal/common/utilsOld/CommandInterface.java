package com.mcal.common.utilsOld;

public interface CommandInterface {

    public boolean runCommand(String command, String[] env, Integer timeout);

    public boolean runCommand(String command, String[] env, Integer timeout, boolean readWhileExec);

    public String getStdOut();

    public String getStdError();
}
