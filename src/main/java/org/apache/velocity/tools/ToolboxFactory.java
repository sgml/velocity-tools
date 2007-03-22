package org.apache.velocity.tools;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.velocity.tools.config.Data;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.apache.velocity.tools.config.ToolboxConfiguration;
import org.apache.velocity.tools.config.ToolConfiguration;

/**
 * 
 *
 * @author Nathan Bubna
 * @version $Id: ToolboxFactory.java 511959 2007-02-26 19:24:39Z nbubna $
 */
public class ToolboxFactory
{
    public static final String DEFAULT_SCOPE = "request";
    public static final String APPLICATION_SCOPE = "application";

    private Map<String,Map<String,ToolInfo>> scopedToolInfo;
    private Map<String,Map<String,Object>> scopedProperties;
    private Map<String,Object> data;
    private Map<String,Object> globalProperties;
    
    public ToolboxFactory()
    {
        this.scopedToolInfo = new HashMap<String,Map<String,ToolInfo>>();
        this.scopedProperties = new HashMap<String,Map<String,Object>>();
    }


    public synchronized void configure(FactoryConfiguration config)
    {
        if (!config.isValid())
        {
            throw new IllegalArgumentException("FactoryConfiguration is not valid");
        }

        // first do the easy part and add any data
        for (Data datum : config.getData())
        {
            putData(datum.getKey(), datum.getConvertedValue());
        }

        // property precedence follows two rules:
        //      newer Foo-level props beat older ones
        //      narrower-scoped props beat broader-scoped ones

        // next add the toolboxes
        for (ToolboxConfiguration toolbox : config.getToolboxes())
        {
            String scope = toolbox.getScope();

            // starting with the toolinfo
            for (ToolConfiguration tool : toolbox.getTools())
            {
                addToolInfo(scope, tool.createInfo());
            }

            // then add the properties for this toolbox
            Map<String,Object> newToolboxProps = toolbox.getProperties();
            putProperties(scope, newToolboxProps);

            // now go thru all toolinfo for this scope old and new
            for (ToolInfo info : getToolInfo(scope).values())
            {
                // and add these new toolbox properties, which have
                // lower precedence than the props already in the info
                info.addProperties(newToolboxProps);
            }
        }

        // now set all the factory-level properties
        // new ones will override old ones
        Map<String,Object> newGlobalProps = config.getProperties();
        putGlobalProperties(newGlobalProps);

        // now go thru all toolboxes in this factory
        for (Map<String,ToolInfo> toolbox : scopedToolInfo.values())
        {
            // iterating over all the toolinfo in them
            for (ToolInfo info : toolbox.values())
            {
                // and adding the new global properties last
                // since they have the lowest precedence
                info.addProperties(newGlobalProps);
            }
        }
    }



    protected synchronized Object putData(String key, Object value)
    {
        if (data == null)
        {
            data = new HashMap<String,Object>();
        }
        return data.put(key, value);
    }

    protected void addToolInfo(String scope, ToolInfo tool)
    {
        //TODO? check the scope against any "ValidScopes"
        //      annotation on the tool class, or do we leave
        //      validation like this to FactoryConfiguration?
        getToolInfo(scope).put(tool.getKey(), tool);
    }

    protected synchronized Map<String,ToolInfo> getToolInfo(String scope)
    {
        Map<String,ToolInfo> tools = scopedToolInfo.get(scope);
        if (tools == null)
        {
            tools = new HashMap<String,ToolInfo>();
            scopedToolInfo.put(scope, tools);
        }
        return tools;
    }

    protected synchronized void putGlobalProperties(Map<String,Object> props)
    {
        if (props != null && !props.isEmpty())
        {
            if (globalProperties == null)
            {
                globalProperties = new HashMap<String,Object>(props);
            }
            else
            {
                globalProperties.putAll(props);
            }
        }
    }

    protected synchronized void putProperties(String scope, Map<String,Object> props)
    {
        if (props != null && !props.isEmpty())
        {
            Map<String,Object> properties = scopedProperties.get(scope);
            if (properties == null)
            {
                properties = new HashMap<String,Object>(props);
                scopedProperties.put(scope, properties);
            }
            else
            {
                properties.putAll(props);
            }
        }
    }



    public Object getGlobalProperty(String name)
    {
        if (globalProperties == null)
        {
            return null;
        }
        return globalProperties.get(name);
    }

    public Map<String,Object> getData()
    {
        return data;
    }

    public boolean hasToolbox(String scope)
    {
        Map<String,ToolInfo> tools = scopedToolInfo.get(scope);
        return (tools != null && !tools.isEmpty());
    }

    public Toolbox getToolbox(String scope)
    {
        Map<String,ToolInfo> tools = scopedToolInfo.get(scope);
        if (tools == null)
        {
            return null;
        }

        Toolbox toolbox;
        Map properties = scopedProperties.get(scope);
        if (properties == null && globalProperties == null)
        {
            toolbox = new Toolbox(tools);
        }
        else
        {
            if (globalProperties != null)
            {
                properties.putAll(globalProperties);
            }
            toolbox = new Toolbox(tools, properties);
        }

        // if application scoped or if there's only one toolbox,
        // then automatically include data, if we have any.
        if (data != null &&
            (scopedToolInfo.size() == 1 || scope.equals(APPLICATION_SCOPE)))
        {
            toolbox.cacheData(getData());
        }
        return toolbox;
    }

}