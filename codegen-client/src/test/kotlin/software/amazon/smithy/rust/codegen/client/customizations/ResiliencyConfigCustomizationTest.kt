/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rust.codegen.client.customizations

import org.junit.jupiter.api.Test
import software.amazon.smithy.rust.codegen.client.smithy.customizations.ResiliencyConfigCustomization
import software.amazon.smithy.rust.codegen.client.smithy.transformers.OperationNormalizer
import software.amazon.smithy.rust.codegen.client.smithy.transformers.RecursiveShapeBoxer
import software.amazon.smithy.rust.codegen.client.testutil.TestWorkspace
import software.amazon.smithy.rust.codegen.client.testutil.asSmithyModel
import software.amazon.smithy.rust.codegen.client.testutil.rustSettings
import software.amazon.smithy.rust.codegen.client.testutil.testCodegenContext
import software.amazon.smithy.rust.codegen.client.testutil.validateConfigCustomizations

internal class ResiliencyConfigCustomizationTest {
    private val baseModel = """
        namespace test
        use aws.protocols#awsQuery

        structure SomeOutput {
            @xmlAttribute
            someAttribute: Long,

            someVal: String
        }

        operation SomeOperation {
            output: SomeOutput
        }
    """.asSmithyModel()

    @Test
    fun `generates a valid config`() {
        val model = RecursiveShapeBoxer.transform(OperationNormalizer.transform(baseModel))
        val project = TestWorkspace.testProject()
        val codegenContext = testCodegenContext(model, settings = project.rustSettings())

        validateConfigCustomizations(ResiliencyConfigCustomization(codegenContext), project)
    }
}
