package org.gsc.common;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import lombok.extern.slf4j.Slf4j;
import org.gsc.config.Configuration;
import org.junit.Test;

import java.util.List;
import java.util.Map;

@Slf4j
public class ConfigTest {


    @Test
    public void testConfig(){
        Config config = Configuration.getByFileName("","config-localtest.conf");
        System.out.println(config.root().render(ConfigRenderOptions.defaults().setComments(false)));
        for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
            System.out.println("Name:  " + entry.getKey());
            System.out.println(entry);
        }
        List<? extends ConfigObject> list = config.getObjectList("genesis.block.assets");
        for (ConfigObject configObject : list) {
            if (configObject.get("accountName") != null) {
                System.out.println("accountName: " + configObject.get("accountName"));
            }
            if (configObject.get("address") != null) {
                System.out.println("address: " + configObject.get("address"));
            }
        }

    }
}
