package com.rossallenbell.darkfallgearoptimizer;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.rossallenbell.darkfallgearoptimizer.data.CsvArmorProviderTest;

@RunWith(Suite.class)
@SuiteClasses({ ArmorCombinatorTest.class, ArmorSetTest.class, CsvArmorProviderTest.class, ArmorRankerTest.class })
public class AllTests {
    
}
