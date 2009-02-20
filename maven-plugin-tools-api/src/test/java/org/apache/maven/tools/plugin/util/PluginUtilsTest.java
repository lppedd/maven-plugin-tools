package org.apache.maven.tools.plugin.util;

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

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.xml.CompactXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.StringWriter;
import java.util.Collections;

/**
 * @author jdcasey
 */
public class PluginUtilsTest
    extends AbstractMojoTestCase
{
    public void testShouldTrimArtifactIdToFindPluginId()
    {
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "maven-artifactId-plugin" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "maven-plugin-artifactId" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "artifactId-maven-plugin" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "artifactId" ) );
        assertEquals( "artifactId", PluginDescriptor.getGoalPrefixFromArtifactId( "artifactId-plugin" ) );
        assertEquals( "plugin", PluginDescriptor.getGoalPrefixFromArtifactId( "maven-plugin-plugin" ) );
    }

    public void testShouldWriteDependencies()
        throws Exception
    {
        ComponentDependency dependency = new ComponentDependency();
        dependency.setArtifactId( "testArtifactId" );
        dependency.setGroupId( "testGroupId" );
        dependency.setType( "pom" );
        dependency.setVersion( "0.0.0" );

        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setDependencies( Collections.singletonList( dependency ) );

        StringWriter sWriter = new StringWriter();
        XMLWriter writer = new CompactXMLWriter( sWriter );

        PluginUtils.writeDependencies( writer, descriptor );

        String output = sWriter.toString();

        String pattern = "<dependencies>" + "<dependency>" + "<groupId>testGroupId</groupId>"
            + "<artifactId>testArtifactId</artifactId>" + "<type>pom</type>" + "<version>0.0.0</version>"
            + "</dependency>" + "</dependencies>";

        assertEquals( pattern, output );
    }

    public void testShouldFindTwoScriptsWhenNoExcludesAreGiven()
    {
        String testScript = "test.txt";

        String basedir = TestUtils.dirname( testScript );

        String includes = "**/*.txt";

        String[] files = PluginUtils.findSources( basedir, includes );
        assertEquals( 2, files.length );
    }

    public void testShouldFindOneScriptsWhenAnExcludeIsGiven()
    {
        String testScript = "test.txt";

        String basedir = TestUtils.dirname( testScript );

        String includes = "**/*.txt";
        String excludes = "**/*Excludes.txt";

        String[] files = PluginUtils.findSources( basedir, includes, excludes );
        assertEquals( 1, files.length );
    }

    public void testIsMavenReport()
        throws Exception
    {
        try
        {
            PluginUtils.isMavenReport( null, null );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        String impl = "org.apache.maven.tools.plugin.util.stubs.MavenReportStub";

        MavenProjectStub stub = new MavenProjectStub();
        stub.setCompileSourceRoots( Collections.singletonList( getBasedir() + "/target/classes" ) );

        assertTrue( PluginUtils.isMavenReport( impl, stub ) );

        impl = "org.apache.maven.tools.plugin.util.stubs.MojoStub";
        assertFalse( PluginUtils.isMavenReport( impl, stub ) );
    }

    public void testMakeHtmlValid()
    {
        String javadoc = null;
        assertEquals( "", PluginUtils.makeHtmlValid( javadoc ) );
        javadoc = "";
        assertEquals( "", PluginUtils.makeHtmlValid( javadoc ) );

        // true HTML
        javadoc = "Generates <i>something</i> for the project.";
        assertEquals( "Generates <i>something</i> for the project.", PluginUtils.makeHtmlValid( javadoc ) );

        // wrong HTML
        javadoc = "Generates <i>something</i> <b> for the project.";
        assertEquals( "Generates <i>something</i> <b> for the project.</b>", PluginUtils.makeHtmlValid( javadoc ) );

        // wrong XHTML
        javadoc = "Line1<br>Line2";
        assertEquals( "Line1<br/>Line2", PluginUtils.makeHtmlValid( javadoc ).replaceAll( "\\s", "" ) );

        // special characters
        javadoc = "& &amp; < > \u00A0";
        assertEquals( "&amp; &amp; &lt; &gt; \u00A0", PluginUtils.makeHtmlValid( javadoc ) );

        // non ASCII characters
        javadoc = "\u00E4 \u00F6 \u00FC \u00DF";
        assertEquals( javadoc, PluginUtils.makeHtmlValid( javadoc ) );

        // non Latin1 characters
        javadoc = "\u0130 \u03A3 \u05D0 \u06DE";
        assertEquals( javadoc, PluginUtils.makeHtmlValid( javadoc ) );
    }

    public void testDecodeJavadocTags()
    {
        String javadoc = null;
        assertEquals( "", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "";
        assertEquals( "", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@code text}";
        assertEquals( "<code>text</code>", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@code <A&B>}";
        assertEquals( "<code>&lt;A&amp;B&gt;</code>", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal text}";
        assertEquals( "text", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal text}  {@literal text}";
        assertEquals( "text  text", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal <A&B>}";
        assertEquals( "&lt;A&amp;B&gt;", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@link Class}";
        assertEquals( "<code>Class</code>", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class}";
        assertEquals( "Class", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #field}";
        assertEquals( "field", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#field}";
        assertEquals( "Class.field", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method()}";
        assertEquals( "method()", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object arg)}";
        assertEquals( "method()", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object, String)}";
        assertEquals( "method()", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object, String) label}";
        assertEquals( "label", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#method(Object, String)}";
        assertEquals( "Class.method()", PluginUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#method(Object, String) label}";
        assertEquals( "label", PluginUtils.decodeJavadocTags( javadoc ) );
    }

    public void testToText()
        throws Exception
    {
        String javadoc = null;
        assertEquals( "", PluginUtils.toText( javadoc ) );
        javadoc = "";
        assertEquals( "", PluginUtils.toText( javadoc ) );

        // line breaks
        javadoc = "Line1\nLine2";
        assertEquals( "Line1 Line2", PluginUtils.toText( javadoc ) );
        javadoc = "Line1\rLine2";
        assertEquals( "Line1 Line2", PluginUtils.toText( javadoc ) );
        javadoc = "Line1\r\nLine2";
        assertEquals( "Line1 Line2", PluginUtils.toText( javadoc ) );
        javadoc = "Line1<br>Line2";
        assertEquals( "Line1\nLine2", PluginUtils.toText( javadoc ) );

        // true HTML
        javadoc = "Generates <i>something</i> for the project.";
        assertEquals( "Generates something for the project.", PluginUtils.toText( javadoc ) );

        // wrong HTML
        javadoc = "Generates <i>something</i> <b> for the project.";
        assertEquals( "Generates something for the project.", PluginUtils.toText( javadoc ) );

        // javadoc inline tags
        javadoc = "Generates {@code something} for the project.";
        assertEquals( "Generates something for the project.", PluginUtils.toText( javadoc ) );
    }

}
