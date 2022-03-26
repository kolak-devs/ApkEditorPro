/*
 * Copyright 2008 Android4ME
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
 */
package com.mcal.apklib.android.content.res;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.AttributeSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * @author Dmitry Skiba
 *
 */
public interface XmlResourceParser extends XmlPullParser, AttributeSet {
	void close();

	@Override
	default int getAttributeNameResource(int i) {
		return 0;
	}

	@Override
	default int getAttributeListValue(String s, String s1, String[] strings, int i) {
		return 0;
	}

	@Override
	default boolean getAttributeBooleanValue(String s, String s1, boolean b) {
		return false;
	}

	@Override
	default int getAttributeResourceValue(String s, String s1, int i) {
		return 0;
	}

	@Override
	default int getAttributeIntValue(String s, String s1, int i) {
		return 0;
	}

	@Override
	default int getAttributeUnsignedIntValue(String s, String s1, int i) {
		return 0;
	}

	@Override
	default float getAttributeFloatValue(String s, String s1, float v) {
		return 0;
	}

	@Override
	default int getAttributeListValue(int i, String[] strings, int i1) {
		return 0;
	}

	@Override
	default boolean getAttributeBooleanValue(int i, boolean b) {
		return false;
	}

	@Override
	default int getAttributeResourceValue(int i, int i1) {
		return 0;
	}

	@Override
	default int getAttributeIntValue(int i, int i1) {
		return 0;
	}

	@Override
	default int getAttributeUnsignedIntValue(int i, int i1) {
		return 0;
	}

	@Override
	default float getAttributeFloatValue(int i, float v) {
		return 0;
	}

	@Override
	default String getIdAttribute() {
		return null;
	}

	@Override
	default String getClassAttribute() {
		return null;
	}

	@Override
	default int getIdAttributeResourceValue(int i) {
		return 0;
	}

	@Override
	default int getStyleAttribute() {
		return 0;
	}

	@Override
	default void setFeature(String s, boolean b) throws XmlPullParserException {

	}

	@Override
	default boolean getFeature(String s) {
		return false;
	}

	@Override
	default void setProperty(String s, Object o) throws XmlPullParserException {

	}

	@Override
	default Object getProperty(String s) {
		return null;
	}

	@Override
	default void setInput(Reader reader) throws XmlPullParserException {

	}

	@Override
	default void setInput(InputStream inputStream, String s) throws XmlPullParserException {

	}

	@Override
	default String getInputEncoding() {
		return null;
	}

	@Override
	default void defineEntityReplacementText(String s, String s1) throws XmlPullParserException {

	}

	@Override
	default int getNamespaceCount(int i) throws XmlPullParserException {
		return 0;
	}

	@Override
	default String getNamespacePrefix(int i) throws XmlPullParserException {
		return null;
	}

	@Override
	default String getNamespaceUri(int i) throws XmlPullParserException {
		return null;
	}

	@Override
	default String getNamespace(String s) {
		return null;
	}

	@Override
	default int getDepth() {
		return 0;
	}

	@Override
	default String getPositionDescription() {
		return null;
	}

	@Override
	default int getLineNumber() {
		return 0;
	}

	@Override
	default int getColumnNumber() {
		return 0;
	}

	@Override
	default boolean isWhitespace() throws XmlPullParserException {
		return false;
	}

	@Override
	default String getText() {
		return null;
	}

	@Override
	default char[] getTextCharacters(int[] ints) {
		return new char[0];
	}

	@Override
	default String getNamespace() {
		return null;
	}

	@Override
	default String getName() {
		return null;
	}

	@Override
	default String getPrefix() {
		return null;
	}

	@Override
	default boolean isEmptyElementTag() throws XmlPullParserException {
		return false;
	}

	@Override
	default int getAttributeCount() {
		return 0;
	}

	@Override
	default String getAttributeNamespace(int i) {
		return null;
	}

	@Override
	default String getAttributeName(int i) {
		return null;
	}

	@Override
	default String getAttributePrefix(int i) {
		return null;
	}

	@Override
	default String getAttributeType(int i) {
		return null;
	}

	@Override
	default boolean isAttributeDefault(int i) {
		return false;
	}

	@Override
	default String getAttributeValue(int i) {
		return null;
	}

	@Override
	default String getAttributeValue(String s, String s1) {
		return null;
	}

	@Override
	default int getEventType() throws XmlPullParserException {
		return 0;
	}

	@Override
	default int next() throws IOException, XmlPullParserException {
		return 0;
	}

	@Override
	default int nextToken() throws IOException, XmlPullParserException {
		return 0;
	}

	@Override
	default void require(int i, String s, String s1) throws IOException, XmlPullParserException {

	}

	@Override
	default String nextText() throws IOException, XmlPullParserException {
		return null;
	}

	@Override
	default int nextTag() throws IOException, XmlPullParserException {
		return 0;
	}
}