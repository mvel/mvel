package org.mvel2.sh;

import java.util.Map;

public interface CommandSet {
    public Map<String, Command> load();
}
