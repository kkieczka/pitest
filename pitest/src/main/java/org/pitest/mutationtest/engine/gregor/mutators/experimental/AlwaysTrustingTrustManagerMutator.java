package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.util.Log;

import java.util.Arrays;

/**
 * The mutator which sets a body of checkServerTrusted() method of a non-default
 * TrustManager class to simple 'return' statement.
 *
 * Created by Krystian Kieczka on 2017-11-18.
 */
public enum AlwaysTrustingTrustManagerMutator implements MethodMutatorFactory {

    ALWAYS_TRUSTING_TRUST_MANAGER_MUTATOR;

    public class AlwaysTrustingTrustManagerMethodVisitor extends MethodVisitor {

        private final MethodVisitor target;
        private MutationContext context;
        private MethodMutatorFactory factory;
        private String methodName;
        private boolean mutated = false;

        public AlwaysTrustingTrustManagerMethodVisitor(MethodMutatorFactory factory, MethodVisitor methodVisitor,
                                                       MutationContext ctx, String methodName) {
            super(Opcodes.ASM5, null);
            this.target = methodVisitor;
            this.context = ctx;
            this.factory = factory;
            this.methodName = methodName;
        }

        private boolean shouldMutate() {
            return "checkServerTrusted".equals(methodName)
                    && Arrays.asList(context.getClassInfo().getInterfaces()).contains("javax/net/ssl/X509TrustManager")
                    && !(context.getClassInfo().getName().startsWith("javax/net"))
                    && !mutated;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (shouldMutate()) {
                Log.getLogger().warning("ATTMM: mutating...");
                // ensure that tests touching this method will be included during tests execution
                this.context.registerCurrentLine(line);

                final MutationIdentifier newId = this.context.registerMutation(
                        this.factory, "Removed body of custom TrustManager");
                mutated = true;
            }
        }

        @Override
        public void visitCode() {
            if (shouldMutate()) {
                this.target.visitCode();
                this.target.visitInsn(org.objectweb.asm.Opcodes.RETURN);
                // this.target.visitMaxs(4, 1);
                this.target.visitEnd();
            } else {
                this.mv = target;
                this.mv.visitCode();
            }
        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new AlwaysTrustingTrustManagerMethodVisitor(this, methodVisitor, context, methodInfo.getName());
    }

    @Override
    public String getGloballyUniqueId() {
        return this.getName();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
