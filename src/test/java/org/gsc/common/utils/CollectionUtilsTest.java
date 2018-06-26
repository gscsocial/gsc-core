package org.gsc.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class CollectionUtilsTest {


    @Test
    public void testCollectionUtils(){
        List<Integer> list = Arrays.asList(1,2,3,4,5,6,7,8,9,1,6);
        System.out.println(CollectionUtils.collectList(list,i -> i+1));
        System.out.println(CollectionUtils.collectSet(list,i -> i*2));
        System.out.println(CollectionUtils.truncate(list,5));
        System.out.println(CollectionUtils.selectList(list, i -> i>5));
        System.out.println(CollectionUtils.selectSet(list, i -> i>5));
    }

}
