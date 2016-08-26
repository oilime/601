package com.lanan.zigbeetransmission.dataclass;

public class SendPackage {

    private final byte[] data;

    public SendPackage(byte[] dataIn) {
        this.data = dataIn;
    }

    public byte[] getPackageData() {
        return this.data;
    }
}
