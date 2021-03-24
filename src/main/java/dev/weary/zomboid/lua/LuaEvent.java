/*
 * Zomboid Plugin Loader - Java modding tool for Project Zomboid
 * Copyright (C) 2021 00c1
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dev.weary.zomboid.lua;

import se.krka.kahlua.vm.LuaClosure;
import zombie.Lua.Event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dev.weary.zomboid.util.reflect.ReflectionUtil.*;

public class LuaEvent {
    private final int type;
    private final String name;
    private final List<Object> args;
    private final LuaClosure callback;
    private boolean cancelled;

    private static final Field EVENT_INDEX = getField(Event.class, "index");

    public LuaEvent(Event luaEvent, LuaClosure luaClosure, Object[] eventArgs) {
        this.type = getIntValue(luaEvent, EVENT_INDEX);
        this.name = luaEvent.name;
        this.callback = luaClosure;
        this.cancelled = false;
        this.args = new ArrayList<>();

        if (eventArgs != null) {
            this.args.addAll(Arrays.asList(eventArgs));
        }
    }

    public boolean hasName(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Object getArgument(int index) {
        return args.get(index);
    }

    public void setArgument(int index, Object element) {
        args.set(index, element);
    }

    public List<Object> getArgumentList() {
        return Collections.unmodifiableList(args);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean value) {
        cancelled = value;
    }

    public String getLuaFileName() {
        return callback.prototype.filename;
    }
}
