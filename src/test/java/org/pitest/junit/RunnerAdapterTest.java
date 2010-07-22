/*
 * Copyright 2010 Henry Coles
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package org.pitest.junit;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Test;

import com.example.TheoryTest;

public class RunnerAdapterTest {

  private RunnerAdapter testee;

  @Before
  public void setup() {
    this.testee = new RunnerAdapter(TheoryTest.class);
  }

  @Test
  public void testCanBeSerializedAndDeserialized() throws Exception {
    final RunnerAdapter actual = (RunnerAdapter) SerializationUtils
        .clone(this.testee);
    assertEquals(this.testee.getTestUnits().size(), actual.getTestUnits()
        .size());
  }

}