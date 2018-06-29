package org.gsc.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.AddressUtil;
import org.gsc.common.utils.ByteArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Slf4j
public class AddressUtilsTest {

    @Test
    public void addressValid(){
        byte [] address = ByteArray.fromHexString("");
        assertEquals(false,AddressUtil.addressValid(address));

        address = ByteArray.fromHexString("b1422F9bbF7a8d39C263938005c4Da690fb80e6685");
        assertEquals(false,AddressUtil.addressValid(address));

        address = ByteArray.fromHexString("0xb043967252D80bA046C");
        assertEquals(false,AddressUtil.addressValid(address));

        address = ByteArray.fromHexString("b0422F9bbF7a8d39C263938005c4Da690fb80e6685");
        assertEquals(true,AddressUtil.addressValid(address));
        System.out.println(Base58.encode(ByteArray.fromString("b0422F9bbF7a8d39C263938005c4Da690fb80e6685")));
    }

}
