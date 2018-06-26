package org.gsc.config.args;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class GenesisBlockTest {


    @Test(expected = IllegalArgumentException.class)
    public void testThrowIllegalArgumentExceptionWhenSetIllegalTimeStamp(){
        GenesisBlock genesisBlock = new GenesisBlock();
        genesisBlock.setTimestamp("1b29");
    }

}
