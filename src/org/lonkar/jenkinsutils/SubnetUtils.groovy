package org.lonkar.jenkinsutils

@Grab('commons-net:commons-net:3.3')
import org.apache.commons.net.util.SubnetUtils;
import java.io.Serializable;

class SubnetUtils implements Serializable {
  
  /**
   * @returns SubnetUtils.SubnetInfo
   * @throws java.lang.IllegalArgumentException if invalid CIDR block is provided
   */
  static def parse(cidr) {
    return new org.apache.commons.net.util.SubnetUtils(cidr).getInfo();
  }
}