package org.apache.velocity.tools.config;

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

import org.apache.velocity.tools.ToolInfo;
import org.apache.velocity.tools.Utils;

/**
 * 
 *
 * @author Nathan Bubna
 * @version $Id: ToolConfiguration.java 511959 2007-02-26 19:24:39Z nbubna $
 */
public class ToolConfiguration extends Configuration
{
    private String key;
    private String classname;
    private String restrictTo;

    public void setKey(String key)
    {
        this.key = key;
    }

    /**
     * This doesn't take a {@link Class} parameter because
     * this class was not created for all-java configuration.
     */
    public void setClass(String classname)
    {
        this.classname = classname;
    }

    public void setClassname(String classname)
    {
        setClass(classname);
    }

    public void setRestrictTo(String path)
    {
        this.restrictTo = path;
    }

    public String getKey()
    {
        return this.key;
    }

    public String getClassname()
    {
        return this.classname;
    }

    public Class getToolClass()
    {
        try
        {
            return Utils.getClass(getClassname());
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new ConfigurationException(this, cnfe);
        }
    }

    public String[] getInvalidScopes()
    {
        InvalidScope invalid =
            (InvalidScope)getToolClass().getAnnotation(InvalidScope.class);
        if (invalid != null)
        {
            return invalid.value();
        }
        else
        {
            return new String[] {};
        }
    }

    public String[] getValidScopes()
    {
        ValidScope valid =
            (ValidScope)getToolClass().getAnnotation(ValidScope.class);
        if (valid != null)
        {
            return valid.value();
        }
        else
        {
            return new String[] {};
        }
    }

    public String getRestrictTo()
    {
        return this.restrictTo;
    }

    public ToolInfo createInfo()
    {
        ToolInfo info = new ToolInfo(getKey(), getToolClass());
        info.restrictTo(getRestrictTo());
        // it's ok to use this here, because we know it's the
        // first time properties have been added to this ToolInfo
        info.addProperties(getProperties());
        return info;
    }

    @Override
    public void validate()
    {
        super.validate();
        
        // make sure the key is not null
        if (getKey() == null)
        {
            throw new NullKeyException(this);
        }

        // make sure that we can have an accessible tool class
        if (getClassname() == null)
        {
            throw new ConfigurationException(this, "No tool classname has been set for '"+getKey()+'\'');
        }
        else
        {
            getToolClass();
        }
    }

}
