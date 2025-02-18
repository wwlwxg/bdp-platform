/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.qlangtech.tis.plugin.datax.TestDataxMySQLReader;
import com.qlangtech.tis.plugin.datax.TestDataxMySQLReaderDump;
import com.qlangtech.tis.plugin.datax.TestDataxMySQLWriter;
import com.qlangtech.tis.plugin.ds.mysql.TestMySQLDataSourceFactory;
import com.qlangtech.tis.plugin.ds.split.TestDefaultSplitTableStrategy;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author: baisui 百岁
 * @create: 2021-01-07 18:52
 **/
@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestMySQLDataSourceFactory.class //
        , TestDataxMySQLReader.class //
        , TestDataxMySQLWriter.class //
        , TestDataxMySQLReaderDump.class
        , TestDefaultSplitTableStrategy.class})
public class TestAll  //extends TestCase
{

//    public static Test suite() {
//        TestSuite suite = new TestSuite();
//        suite.addTestSuite(TestMySQLDataSourceFactory.class);
//        suite.addTestSuite(TestDataxMySQLReader.class);
//        suite.addTestSuite(TestDataxMySQLWriter.class);
//        return suite;
//    }
}
