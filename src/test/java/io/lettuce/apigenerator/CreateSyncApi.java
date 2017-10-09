/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.apigenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;

import io.lettuce.core.internal.LettuceSets;

/**
 * Create sync API based on the templates.
 *
 * @author Mark Paluch
 */
@RunWith(Parameterized.class)
public class CreateSyncApi {

    private Set<String> FILTER_METHODS = LettuceSets.unmodifiableSet("setAutoFlushCommands", "flushCommands");

    private CompilationUnitFactory factory;

    @Parameterized.Parameters(name = "Create {0}")
    public static List<Object[]> arguments() {
        List<Object[]> result = new ArrayList<>();

        for (String templateName : Constants.TEMPLATE_NAMES) {
            result.add(new Object[] { templateName });
        }

        return result;
    }

    /**
     *
     * @param templateName
     */
    public CreateSyncApi(String templateName) {

        String targetName = templateName;
        File templateFile = new File(Constants.TEMPLATES, "io/lettuce/core/api/" + templateName + ".java");
        String targetPackage;

        if (templateName.contains("RedisSentinel")) {
            targetPackage = "io.lettuce.core.sentinel.api.sync";
        } else {
            targetPackage = "io.lettuce.core.api.sync";
        }

        factory = new CompilationUnitFactory(templateFile, Constants.SOURCES, targetPackage, targetName, commentMutator(),
                methodTypeMutator(), methodFilter(), importSupplier(), null, null);
    }

    /**
     * Mutate type comment.
     *
     * @return
     */
    protected Function<String, String> commentMutator() {
        return s -> s.replaceAll("\\$\\{intent\\}", "Synchronous executed commands") + "* @generated by "
                + getClass().getName() + "\r\n ";
    }

    /**
     * Method filter
     *
     * @return
     */
    protected Predicate<MethodDeclaration> methodFilter() {
        return method -> {
            ClassOrInterfaceDeclaration classOfMethod = (ClassOrInterfaceDeclaration) method.getParentNode();
            if (FILTER_METHODS.contains(method.getName())
                    || FILTER_METHODS.contains(classOfMethod.getName() + "." + method.getName())) {
                return false;
            }

            return true;
        };
    }

    /**
     * Mutate type to async result.
     *
     * @return
     */
    protected Function<MethodDeclaration, Type> methodTypeMutator() {
        return methodDeclaration -> methodDeclaration.getType();
    }

    /**
     * Supply additional imports.
     *
     * @return
     */
    protected Supplier<List<String>> importSupplier() {
        return () -> Collections.emptyList();
    }

    @Test
    public void createInterface() throws Exception {
        factory.createInterface();
    }
}
