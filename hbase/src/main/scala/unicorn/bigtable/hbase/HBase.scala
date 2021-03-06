/*******************************************************************************
 * (C) Copyright 2015 ADP, LLC.
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package unicorn.bigtable.hbase

import java.util.Properties
import scala.collection.JavaConversions._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.TableName
import unicorn.bigtable._
//import unicorn.index.Indexing

/** HBase server adapter.
  *
  * @author Haifeng Li
  */
class HBase(config: Configuration) extends Database[HBaseTable] {
  val connection = ConnectionFactory.createConnection(config)
  val admin = connection.getAdmin

  override def close: Unit = connection.close

  override def apply(name: String): HBaseTable = {
    new HBaseTable(this, name)
  }
/*
  def getTableWithIndex(name: String): HBaseTable with Indexing = {
    new HBaseTable(this, name) with Indexing
  }
*/

  override def tables: Set[String] = {
    admin.listTableNames.filter(!_.isSystemTable).map(_.getNameAsString).toSet
  }

  override def createTable(name: String, props: Properties, families: String*): HBaseTable = {
    if (admin.tableExists(TableName.valueOf(name)))
      throw new IllegalStateException(s"Creates Table $name, which already exists")
    
    val tableDesc = new HTableDescriptor(TableName.valueOf(name))
    props.stringPropertyNames.foreach { p => tableDesc.setConfiguration(p, props.getProperty(p))}
    families.foreach { family =>
      val desc = new HColumnDescriptor(family)
      props.stringPropertyNames.foreach { p => desc.setConfiguration(p, props.getProperty(p))}
      tableDesc.addFamily(desc)
    }
    admin.createTable(tableDesc)
    apply(name)
  }

  override def dropTable(name: String): Unit = {
    val tableName = TableName.valueOf(name)
    admin.disableTable(tableName)
    admin.deleteTable(tableName)
  }

  /** Truncates a table and preserves the splits */
  override def truncateTable(name: String): Unit = {
    admin.truncateTable(TableName.valueOf(name), true)
  }

  override def tableExists(name: String): Boolean = {
    admin.tableExists(TableName.valueOf(name))
  }

  override def compactTable(name: String): Unit = {
    val tableName = TableName.valueOf(name)
    admin.majorCompact(tableName)
  }
}

object HBase {
  /* Uses hbase-site.xml and in hbase-default.xml that can be found on the CLASSPATH */
  def apply(): HBase = {
    val config = HBaseConfiguration.create
    new HBase(config)
  }

  def apply(config: Configuration): HBase = {
    new HBase(config)
  }
}
