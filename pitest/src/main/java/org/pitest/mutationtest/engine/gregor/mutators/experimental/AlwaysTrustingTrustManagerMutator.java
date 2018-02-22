package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

import java.util.Arrays;

/**
 * Mutator which replaces 'throw' instructions with returns for implementations of
 * X509TrustManager.checkServerTrusted()
 *
 * Created by Krystian Kieczka on 2018-02-19.
 */
public enum AlwaysTrustingTrustManagerMutator implements MethodMutatorFactory {
    ALWAYS_TRUSTING_TRUST_MANAGER_MUTATOR;

    public class AlwaysTrustingTrustManagerMethodVisitor extends MethodVisitor {

        private MethodMutatorFactory factory;
        private MutationContext context;
        private MethodInfo methodInfo;

        public AlwaysTrustingTrustManagerMethodVisitor(MethodMutatorFactory factory, MethodInfo methodInfo,
                                                       MethodVisitor methodVisitor, MutationContext ctx) {
            super(Opcodes.ASM6, methodVisitor);
            this.factory = factory;
            this.context = ctx;
            this.methodInfo = methodInfo;
        }

        private boolean isCheckServerTrustedMethod() {
            return methodInfo.getName().equals("checkServerTrusted")
                    && Arrays.asList(context.getClassInfo().getInterfaces()).contains("javax/net/ssl/X509TrustManager")
                    && !(context.getClassInfo().getName().startsWith("javax/net"));
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.ATHROW && isCheckServerTrustedMethod()) {
                final MutationIdentifier newId = this.context.registerMutation(
                        this.factory, "Changed exception throw to return");
                if (this.context.shouldMutate(newId)) {
                    this.mv.visitInsn(Opcodes.RETURN);
                } else {
                    super.visitInsn(opcode);
                }
            } else {
                super.visitInsn(opcode);
            }
        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new AlwaysTrustingTrustManagerMethodVisitor(this, methodInfo, methodVisitor, context);
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
