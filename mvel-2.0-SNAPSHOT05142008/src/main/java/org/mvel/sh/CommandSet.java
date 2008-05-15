package org.mvel.sh;

import java.util.Map;

public interface CommandSet {
    public Map<String, Command> load();
}
