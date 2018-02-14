package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.pitest.functional.F2;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.mutationtest.engine.gregor.mutators.MethodCallMethodVisitor;

/**
 * Experimental mutator which removes calls to XmlPullParser.require() methods.
 *
 * Created by Krystian Kieczka on 2018-02-14.
 */
public enum XMLRemoveRequireMutator implements MethodMutatorFactory {
    XML_REMOVE_REQUIRE_MUTATOR;

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new MethodCallMethodVisitor(methodInfo, context, methodVisitor, this, xmlRequireMethods());
    }

    @Override
    public String getGloballyUniqueId() {
        return this.getName();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    private static F2<String, String, Boolean> xmlRequireMethods() {
        return new F2<String, String, Boolean>() {

            @Override
            public Boolean apply(final String name, final String desc) {
                return "require".equals(name) && "(ILjava/lang/String;Ljava/lang/String;)V".equals(desc);
            }
        };
    }
}
