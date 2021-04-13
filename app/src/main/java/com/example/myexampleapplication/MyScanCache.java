package com.example.myexampleapplication;

import java.util.Collection;
import java.util.HashMap;

public class MyScanCache {

    public class Device {

        private String Address;
        private String Alias;
        private String Name;

        public Device(String Address, String Name) {
            this.Address = Address;
            this.Name = (Name != null) ? Name : "unnamed";
            this.Alias= "";
        }

        public Device (String Address, String Alias, String Name) {
            this(Address, Name);
            if (Alias != null) {
                this.Alias = Alias;
            }
        }

        public String getName() {
            return Name;
        }

        public String getAddress() {
            return Address;
        }

        public String view() {
            String printable = "";
            printable += Address;
            if (!Alias.equals("")) {
                printable += " (" + Alias + ")";
            }
            printable += " " + Name;
            return printable;
        }
    }

    private HashMap<String, Device> cache;

    public MyScanCache() {
        cache = new HashMap<>();
    }

    public void addDevice(Device dev) {
        cache.put(dev.getAddress(), dev);
    }

    public void addDevice(String address, String alias, String name) {
        this.addDevice(new Device(address, alias, name));
    }

    public void clear() {
        cache.clear();
    }

    public Collection<Device> values() {
        return cache.values();
    }

    public int count() {
        return cache.size();
    }
}
