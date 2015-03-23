/**
 *
 * Copyright (c) 2006-2015, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.orm.code.model.java;

import com.speedment.codegen.base.CodeGenerator;
import com.speedment.codegen.lang.controller.AutoImports;
import com.speedment.codegen.lang.models.AnnotationUsage;
import com.speedment.codegen.lang.models.ClassOrInterface;
import com.speedment.codegen.lang.models.Constructor;
import com.speedment.codegen.lang.models.Field;
import com.speedment.codegen.lang.models.File;
import com.speedment.codegen.lang.models.Interface;
import com.speedment.codegen.lang.models.Javadoc;
import com.speedment.codegen.lang.models.Type;
import static com.speedment.codegen.lang.models.constants.DefaultAnnotationUsage.GENERATED;
import static com.speedment.codegen.lang.models.constants.DefaultJavadocTag.AUTHOR;
import com.speedment.codegen.lang.models.implementation.FileImpl;
import com.speedment.codegen.lang.models.implementation.JavadocImpl;
import com.speedment.codegen.lang.models.values.TextValue;
import com.speedment.orm.config.model.Column;
import com.speedment.orm.config.model.ConfigEntity;
import com.speedment.orm.config.model.Dbms;
import com.speedment.orm.config.model.ForeignKey;
import com.speedment.orm.config.model.Index;
import com.speedment.orm.config.model.Project;
import com.speedment.orm.config.model.Schema;
import com.speedment.orm.config.model.Table;
import com.speedment.orm.config.model.aspects.Node;
import com.speedment.util.Beans;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 *
 * @author pemi
 * @param <C> ConfigEntity type.
 * @param <J> Java type (Interface or Class) to generate
 */
public abstract class DefaultJavaClassTranslator<C extends ConfigEntity, J extends ClassOrInterface<J>> implements JavaClassTranslator<C> {

    public static final String GETTER_METHOD_PREFIX = "get";
    public static final String SETTER_METHOD_PREFIX = "set";
    public static final String BUILDER_METHOD_PREFIX = SETTER_METHOD_PREFIX;

    public static final String GENERATED_JAVADOC_MESSAGE = "\n<p>\nThis Class or Interface has been automatically generated by Speedment.\n"
            + "Any changes made to this Class or Interface will be overwritten.";

    private final C configEntity;
    private final CodeGenerator codeGenerator;

    public DefaultJavaClassTranslator(CodeGenerator codeGenerator, C configEntity) {
        this.configEntity = configEntity;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public C getNode() {
        return configEntity;
    }

    protected AnnotationUsage generated() {
        return GENERATED.set(new TextValue("Speedment"));
    }

    protected abstract String getFileName();

    protected abstract J make(File file);

    @Override
    public File get() {
        final File file = new FileImpl(baseDirectoryName() + "/" + (isInImplPackage() ? "impl/" : "") + getFileName() + ".java");
        final J item = make(file);
        item.set(getJavaDoc());
        file.add(item);
        file.call(new AutoImports(getCodeGenerator().getDependencyMgr()));
        return file;
    }

    protected abstract String getJavadocRepresentText();

    protected Javadoc getJavaDoc() {
        return new JavadocImpl(getJavadocRepresentText() + " representing an entity (for example, a row) in the " + getNode().toString() + "." + GENERATED_JAVADOC_MESSAGE)
                .add(AUTHOR.setValue("Speedment"));
    }

    public CodeGenerator getCodeGenerator() {
        return codeGenerator;
    }

    protected boolean isInImplPackage() {
        return false;
    }

    protected abstract class Builder<T extends ClassOrInterface<T>> {

        private final String name;
        private final Map<Class<?>, List<BiConsumer<T, ? extends Node>>> map;

        public Builder(String name) {
            this.name = name;
            this.map = new HashMap<>();
        }

        public Builder<T> addProjectConsumer(BiConsumer<T, Project> consumer) {
            return Beans.run(this, () -> aquireListAndAdd(Project.class, consumer));
        }

        public Builder<T> addDbmsConsumer(BiConsumer<T, Dbms> consumer) {
            return Beans.run(this, () -> aquireListAndAdd(Dbms.class, consumer));
        }

        public Builder<T> addSchemaConsumer(BiConsumer<T, Schema> consumer) {
            return Beans.run(this, () -> aquireListAndAdd(Schema.class, consumer));
        }

        public Builder<T> addTableConsumer(BiConsumer<T, Table> consumer) {
            return Beans.run(this, () -> aquireListAndAdd(Table.class, consumer));
        }

        public Builder<T> addColumnConsumer(BiConsumer<T, Column> consumer) {
            return Beans.run(this, () -> aquireListAndAdd(Column.class, consumer));
        }

        public Builder<T> addIndexConsumer(BiConsumer<T, Index> consumer) {
            return Beans.run(this, () -> aquireListAndAdd(Index.class, consumer));
        }

        public Builder<T> addForeignKeyConsumer(BiConsumer<T, ForeignKey> consumer) {
            return Beans.run(this, () -> aquireListAndAdd(ForeignKey.class, consumer));
        }

        @SuppressWarnings("unchecked")
        protected <C extends Node> void aquireListAndAdd(Class<C> clazz, BiConsumer<T, C> consumer) {
            aquireList(clazz)
                    .add((BiConsumer<T, Node>) consumer);
        }

        @SuppressWarnings("unchecked")
        protected <C extends Node> List<BiConsumer<T, C>> aquireList(Class<?> clazz) {
            return (List<BiConsumer<T, C>>) (List<?>) map.computeIfAbsent(clazz, $ -> new ArrayList<>());
        }

        public void act(T item, Node node) {
            aquireList(node.getInterfaceMainClass())
                    .forEach(c -> c.accept(item, node));
        }

        public abstract T newInstance(String name);

        public T build() {
            final T i = newInstance(name);
            act(i, project());
            act(i, dbms());
            act(i, schema());
            act(i, table());
            table().stream().forEachOrdered(c -> act(i, c));
            i.add(generated());
            return i;
        }

    }

    protected class ClassBuilder extends Builder<com.speedment.codegen.lang.models.Class> {

        public ClassBuilder(String name) {
            super(name);
        }

        @Override
        public com.speedment.codegen.lang.models.Class newInstance(String name) {
            return com.speedment.codegen.lang.models.Class.of(name);
        }

    }

    protected class InterfaceBuilder extends Builder<Interface> {

        public InterfaceBuilder(String name) {
            super(name);
        }

        @Override
        public Interface newInstance(String name) {
            return Interface.of(name);
        }

    }

    public Field fieldFor(Column c) {
        return Field.of(variableName(c), Type.of(c.getMapping()));
    }

    public Constructor emptyConstructor() {
        return Constructor.of().public_();
    }

    public enum CopyConstructorMode {

        SETTER, FIELD, BUILDER;
    }

    public Constructor copyConstructor(Type type, CopyConstructorMode mode) {
        return with(Constructor.of().public_().add(Field.of(variableName(), type).final_()), constructor -> {
            columns().forEachOrdered(c -> {
                switch (mode) {
                    case FIELD: {
                        constructor.add("this." + variableName(c) + " = " + variableName() + ".get" + typeName(c) + "();");
                        break;
                    }
                    case SETTER: {
                        constructor.add(SETTER_METHOD_PREFIX + typeName(c) + "(" + variableName() + ".get" + typeName(c) + "());");
                        break;
                    }
                    case BUILDER: {
                        constructor.add(BUILDER_METHOD_PREFIX + typeName(c) + "(" + variableName() + ".get" + typeName(c) + "());");
                        break;
                    }
                }
            });
        });

    }

}
