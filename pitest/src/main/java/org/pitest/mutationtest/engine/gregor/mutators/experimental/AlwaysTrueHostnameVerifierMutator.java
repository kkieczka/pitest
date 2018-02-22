package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

import java.util.Arrays;

/**
 * Mutator which changes return values of HostnameVerifier.verify() method.
 * If return value is other than true, changes it to true.
 *
 * Created by Krystian Kieczka on 2018-02-19.
 */
public enum AlwaysTrueHostnameVerifierMutator implements MethodMutatorFactory {
    ALWAYS_TRUE_HOSTNAME_VERIFIER_MUTATOR;

    public class AlwaysTrueHostnameVerifierMethodVisitor extends MethodVisitor {

        private MethodMutatorFactory factory;
        private MutationContext context;
        private boolean isVerifyMethod;
        private boolean trueWasLoaded;

        public AlwaysTrueHostnameVerifierMethodVisitor(MethodMutatorFactory factory, MethodInfo methodInfo,
                                                       MethodVisitor methodVisitor, MutationContext ctx) {
            super(Opcodes.ASM6, methodVisitor);
            this.factory = factory;
            this.context = ctx;
            this.trueWasLoaded = false;
            this.isVerifyMethod = methodInfo.getName().equals("verify")
                    && Arrays.asList(context.getClassInfo().getInterfaces()).contains("javax/net/ssl/HostnameVerifier")
                    && !(context.getClassInfo().getName().startsWith("javax/net"));
        }

        @Override
        public void visitInsn(int opcode) {
            if (isVerifyMethod) {
                if (opcode == Opcodes.ICONST_1) {
                    trueWasLoaded = true;
                    super.visitInsn(opcode);

                } else if (opcode == Opcodes.IRETURN) {
                    if (!trueWasLoaded) {
                        final MutationIdentifier newId = this.context.registerMutation(
                                this.factory, "Changed return value of verify() to true");
                        if (this.context.shouldMutate(newId)) {
                            super.visitInsn(Opcodes.POP);
                            super.visitInsn(Opcodes.ICONST_1);
                        }
                    }
                    super.visitInsn(opcode);

                } else {
                    trueWasLoaded = false;
                    super.visitInsn(opcode);
                }
            } else {
                super.visitInsn(opcode);
            }
        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new AlwaysTrueHostnameVerifierMethodVisitor(this, methodInfo, methodVisitor, context);
    }

    @Override
    public String getGloballyUniqueId() {
        return getName();
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
