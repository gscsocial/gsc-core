package org.gsc.config.args;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Parameter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class WitnessTest {
    private Witness witness = new Witness();

    @Before
    public void setWitness(){
        witness.setAddress(ByteArray.fromHexString(Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET+"1e9a53b2df0cdab34156f0aecdf60c1c10b4567"));
        witness.setUrl("http://gsc.org");
        witness.setVoteCount(100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionWhenSetNullAddress(){
        witness.setAddress(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionWhenSetEmptyAddress(){
        witness.setAddress(ByteArray.fromHexString(""));
    }
    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionWhenSetIllegalAddress(){
        witness.setAddress(ByteArray.fromHexString("1e90a4"));
    }

    @Test
    public void setAddressRight() {
        witness
                .setAddress(ByteArray.fromHexString(
                        Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "558d53b2df0cd78158f6f0aecdf60c1c10b15413"));
        Assert.assertEquals(
                Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "558d53b2df0cd78158f6f0aecdf60c1c10b15413",
                ByteArray.toHexString(witness.getAddress()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionWhenSetNullUrl() {
        witness.setUrl(null);
    }

    @Test
    public void setUrlCorrectly(){
        witness.setUrl("http://allen.org");
    }


    @Test
    public void setVoteCountRight() {
        witness.setVoteCount(Long.MAX_VALUE);
        Assert.assertEquals(Long.MAX_VALUE, witness.getVoteCount());

        witness.setVoteCount(Long.MIN_VALUE);
        Assert.assertEquals(Long.MIN_VALUE, witness.getVoteCount());

        witness.setVoteCount(1000L);
        Assert.assertEquals(1000L, witness.getVoteCount());
    }

    @Test
    public void getVoteCountRight() {
        Assert.assertEquals(1000L, witness.getVoteCount());
    }
}