package appeng.helpers;

import appeng.util.nbt.NBTFilterConfig;

public interface INBTFilterable {

    NBTFilterConfig getNBTFilterConfig();

    void setNBTFilterConfig(NBTFilterConfig config);
}
