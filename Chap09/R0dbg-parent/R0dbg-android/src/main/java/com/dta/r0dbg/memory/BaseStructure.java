package com.dta.r0dbg.memory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

public abstract class BaseStructure implements IStructure {
    private Pointer pointer;

    public BaseStructure(Pointer pointer) {
        if (pointer == null) {
            return;
        }
        this.pointer = pointer;
        List<String> fieldOrder = getFieldOrder();
        for (String field : fieldOrder) {
            try {
                Field f = this.getClass().getField(field);
                if (BaseStructure.class.isAssignableFrom(f.getType())) {
                    Constructor<?> constructor = f.getType().getConstructor(Pointer.class);
                    Object o = constructor.newInstance(pointer);
                    f.set(this, o);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int write() {
        List<String> fieldOrder = getFieldOrder();
        int offset = 0;
        for (String field : fieldOrder) {
            try {
                Field f = this.getClass().getField(field);

                int fieldAlignment = getFileAlignment(f);

                if ((offset % fieldAlignment) != 0) {
                    offset += fieldAlignment - (offset % fieldAlignment);
                }

                if (BaseStructure.class.isAssignableFrom(f.getType())) {
                    //处理递归情况
                    BaseStructure child = (BaseStructure) f.get(this);
                    Field f_pointer = BaseStructure.class.getDeclaredField("pointer");
                    f_pointer.setAccessible(true);
                    f_pointer.set(child,pointer.share(offset));
                    int write = child.write();
                    offset+=write;
                    continue;
                }
                Object value = f.get(this);

                if (int.class.isAssignableFrom(f.getType())) {
                    pointer.setInt(offset, (int) value);
                } else if (long.class.isAssignableFrom(f.getType())) {
                    pointer.setLong(offset, (long) value);
                } else if (byte.class.isAssignableFrom(f.getType())) {
                    pointer.write(offset, new byte[]{(byte) value},0,1);
                } else if (f.getType().getName().equals("[B")) {
                    byte[] bytes = (byte[]) value;
                    pointer.write(offset, bytes,0,bytes.length);
                }
                offset += fieldAlignment;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return offset;
    }

    protected int getFileAlignment(Field f){
        if (int.class.isAssignableFrom(f.getType())) {
            return 4;
        } else if (long.class.isAssignableFrom(f.getType())) {
            return 8;
        } else if (byte.class.isAssignableFrom(f.getType())) {
            return 1;
        } else if (f.getType().getName().equals("[B")) {
            try {
                byte[] bytes = (byte[]) f.get(this);
                return ( ( ( bytes.length - 1 ) /  4 ) + 1 ) * 4;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return 4;
        }
        return 1;
    }

    public abstract List<String> getFieldOrder();
}
