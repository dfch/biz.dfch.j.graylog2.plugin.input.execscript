package biz.dfch.j.graylog2.plugin.input;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the plugin. Your class should implement one of the existing plugin
 * interfaces. (i.e. AlarmCallback, MessageInput, MessageOutput)
 */
public class dfchBizExecScript extends MessageInput
{
    private static final String DF_PLUGIN_NAME = "d-fens SCRIPT Input";
    private static final String DF_PLUGIN_HUMAN_NAME = "biz.dfch.j.graylog2.plugin.input.execscript";
    private static final String DF_PLUGIN_DOC_LINK = "https://github.com/dfch/biz.dfch.j.graylog2.plugin.input.execscript";

    private static final String DF_SCRIPT_ENGINE = "DF_SCRIPT_ENGINE";
    private static final String DF_SCRIPT_PATH_AND_NAME = "DF_SCRIPT_PATH_AND_NAME";
    private static final String DF_DISPLAY_SCRIPT_OUTPUT = "DF_DISPLAY_SCRIPT_OUTPUT";
    private static final String DF_SCRIPT_CACHE_CONTENTS = "DF_SCRIPT_CACHE_CONTENTS";

    private boolean _isRunning = false;
    private boolean _isExclusive = false;
    private Configuration _configuration;
    private Map<String, Object> _attributes = Maps.newHashMap();

    private static final ScriptEngineManager _scriptEngineManager = new ScriptEngineManager();
    private static ScriptEngine _scriptEngine;
    private static ScriptContext _scriptContext;
    private File _file;

    private MetricRegistry metricRegistry = null;
    private NodeId nodeId = null;
    private Object client;
    private List<String> topics;
    
    private static AtomicInteger _counter = new AtomicInteger(0);

    private static final Logger LOG = LoggerFactory.getLogger(dfchBizExecScript.class);

//    @Inject
//    public dfchBizExecScript(final MetricRegistry metricRegistry, final NodeId nodeId)
//    {
//        this(metricRegistry, nodeId, null, null);
//    }

    public dfchBizExecScript()
    {
        // N/A
        String s = "*** " + DF_PLUGIN_NAME + "::dfchBizExecScript()\r\n";
        LOG.trace(s);
        System.out.printf(s);
    }
//    @VisibleForTesting
//    dfchBizExecScript(final MetricRegistry metricRegistry, final NodeId nodeId,
//              final dfchBizExecScript client, final List<String> topics)
//    {
//        this.metricRegistry = metricRegistry;
//        this.nodeId = nodeId;
//        this.client = client;
//        this.topics = topics;
//    }

    public void initialize(Configuration configuration) 
    {
        try
        {
            String s = "*** " + DF_PLUGIN_NAME + "::initialize()\n";
            LOG.trace(s);
            System.out.printf(s);
            
            _configuration = configuration;
            _isRunning = true;

            LOG.trace("DF_SCRIPT_ENGINE         : %s\r\n", _configuration.getString("DF_SCRIPT_ENGINE"));
            LOG.trace("DF_SCRIPT_PATH_AND_NAME  : %s\r\n", _configuration.getString("DF_SCRIPT_PATH_AND_NAME"));
            LOG.trace("DF_DISPLAY_SCRIPT_OUTPUT : %b\r\n", _configuration.getBoolean("DF_DISPLAY_SCRIPT_OUTPUT"));
            LOG.trace("DF_SCRIPT_CACHE_CONTENTS : %b\r\n", _configuration.getBoolean("DF_SCRIPT_CACHE_CONTENTS"));
            System.out.printf("DF_SCRIPT_ENGINE         : %s\r\n", _configuration.getString("DF_SCRIPT_ENGINE"));
            System.out.printf("DF_SCRIPT_PATH_AND_NAME  : %s\r\n", _configuration.getString("DF_SCRIPT_PATH_AND_NAME"));
            System.out.printf("DF_DISPLAY_SCRIPT_OUTPUT : %b\r\n", _configuration.getBoolean("DF_DISPLAY_SCRIPT_OUTPUT"));
            System.out.printf("DF_SCRIPT_CACHE_CONTENTS : %b\r\n", _configuration.getBoolean("DF_SCRIPT_CACHE_CONTENTS"));

            _file = new File(_configuration.getString("DF_SCRIPT_PATH_AND_NAME"));
            _scriptEngine = _scriptEngineManager.getEngineByName(_configuration.getString("DF_SCRIPT_ENGINE"));
            _scriptContext = _scriptEngine.getContext();

            super.initialize(configuration);
        }
        catch(Exception ex)
        {
            _isRunning = false;

            LOG.error("*** " + DF_PLUGIN_NAME + "::initialize() - Exception");
            ex.printStackTrace();
        }
    }
    
    @Override
    public void checkConfiguration(Configuration configuration) throws ConfigurationException
    {
        try
        {
            String s = "*** " + DF_PLUGIN_NAME + "::checkConfiguration()";
            LOG.trace(s);

            LOG.trace("DF_SCRIPT_ENGINE         : %s\r\n", _configuration.getString("DF_SCRIPT_ENGINE"));
            LOG.trace("DF_SCRIPT_PATH_AND_NAME  : %s\r\n", _configuration.getString("DF_SCRIPT_PATH_AND_NAME"));
            LOG.trace("DF_DISPLAY_SCRIPT_OUTPUT : %b\r\n", _configuration.getBoolean("DF_DISPLAY_SCRIPT_OUTPUT"));
            LOG.trace("DF_SCRIPT_CACHE_CONTENTS : %b\r\n", _configuration.getBoolean("DF_SCRIPT_CACHE_CONTENTS"));
            System.out.printf("DF_SCRIPT_ENGINE         : %s\r\n", _configuration.getString("DF_SCRIPT_ENGINE"));
            System.out.printf("DF_SCRIPT_PATH_AND_NAME  : %s\r\n", _configuration.getString("DF_SCRIPT_PATH_AND_NAME"));
            System.out.printf("DF_DISPLAY_SCRIPT_OUTPUT : %b\r\n", _configuration.getBoolean("DF_DISPLAY_SCRIPT_OUTPUT"));
            System.out.printf("DF_SCRIPT_CACHE_CONTENTS : %b\r\n", _configuration.getBoolean("DF_SCRIPT_CACHE_CONTENTS"));
        }
        catch(Exception ex)
        {
            //_isRunning = false;

            LOG.error("*** " + DF_PLUGIN_NAME + "::checkConfiguration() - Exception");
            ex.printStackTrace();
        }
    }

    @Override
    public void launch(final Buffer buffer) throws MisfireException
    {
        System.out.printf("*** " + DF_PLUGIN_NAME + "::launch()\r\n");
        try
        {
//            while(_isRunning)
//            {
                //wait(250);

                DateTime dateTime = new DateTime();
                String msgString = String.format("%d: %s, %s", _counter.incrementAndGet(), _configuration.getString("DF_PLUGIN_NAME"), dateTime.toString());
                Message msg = new Message(msgString, _configuration.getString("DF_PLUGIN_NAME"), dateTime);
                buffer.insertCached(msg, dfchBizExecScript.this);
//            }
        }
//        catch(MisfireException ex)
//        {
//            LOG.error("*** " + DF_PLUGIN_NAME + "::launch() - MisfireException");
//            ex.printStackTrace();
//        }
        catch(Exception ex)
        {
            LOG.error("*** " + DF_PLUGIN_NAME + "::launch() - Exception");
            System.out.printf("*** " + DF_PLUGIN_NAME + "::launch() - Exception\r\n");
            ex.printStackTrace();
        }
    }
    
    @Override
    public Map<String, Object> getAttributes() 
    {
        return Maps.transformEntries(getConfiguration().getSource(), new Maps.EntryTransformer<String, Object, Object>() {
            @Override
            public Object transformEntry(String key, Object value) {
                return value;
            }
        });
    }    
    @Override
    public void stop()
    {
        _isRunning = false;
    }

    @Override
    public boolean isExclusive()
    {
        return _isExclusive;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration()
    {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();

        configurationRequest.addField(new TextField
                        (
                                DF_SCRIPT_ENGINE
                                ,
                                "Script Engine"
                                ,
                                "javascript"
                                ,
                                "Specify the name of the script engine to use."
                                ,
                                ConfigurationField.Optional.NOT_OPTIONAL
                        )
        );
        configurationRequest.addField(new TextField
                        (
                                DF_SCRIPT_PATH_AND_NAME
                                ,
                                "Script Path"
                                ,
                                "/opt/graylog2/plugin/bizDfchMessageinput.js"
                                ,
                                "Specify the full path and name of the script to execute."
                                ,
                                ConfigurationField.Optional.NOT_OPTIONAL
                        )
        );
        configurationRequest.addField(new BooleanField
                        (
                                DF_DISPLAY_SCRIPT_OUTPUT
                                ,
                                "Show script output"
                                ,
                                false
                                ,
                                "Show the script output on the console."
                        )
        );
        configurationRequest.addField(new BooleanField
                        (
                                DF_SCRIPT_CACHE_CONTENTS
                                ,
                                "Cache script contents"
                                ,
                                true
                                ,
                                "Cache the contents of the script upon plugin initialisation."
                        )
        );
        return configurationRequest;
    }

    @Override
    public String getName()
    {
        return DF_PLUGIN_NAME;
    }

//    @Override
//    public String getHumanName()
//    {
//        return DF_PLUGIN_HUMAN_NAME;
//    }

    @Override
    public String linkToDocs()
    {
        return DF_PLUGIN_DOC_LINK;
    }
}

/**
 *
 *
 * Copyright 2015 Ronald Rink, d-fens GmbH
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
 *
 */
