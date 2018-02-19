package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

import java.util.Arrays;

/**
 * The mutator which sets a body of verify() method from a non-default
 * HostnameVerifier class so that it always returns true.
 *
 * Created by Krystian Kieczka on 2017-08-08.
 */
public enum AlwaysTrueHostnameVerifierMutator implements MethodMutatorFactory {

    ALWAYS_TRUE_HOSTNAME_VERIFIER_MUTATOR;

    public class AlwaysTrueHostnameVerifierMethodVisitor extends MethodVisitor {

        //private final MethodVisitor target;
        private MutationContext context;
        private MethodMutatorFactory factory;
        private String methodName;
        private boolean mutated = false;

        public AlwaysTrueHostnameVerifierMethodVisitor(MethodMutatorFactory factory, MethodVisitor methodVisitor,
                                                       MutationContext ctx, String methodName) {
            super(Opcodes.ASM6, methodVisitor);
            //this.target = methodVisitor;
            this.context = ctx;
            this.factory = factory;
            this.methodName = methodName;
        }

        // As PITest requires information regarding which line was modified by a given mutator,
        // set the context to the first line of the verify() method before creating a mutation.
        // In this case every test which touches verify() should be taken under consideration
        // as potential mutation killer (as every invocation of a function must execute at least
        // its first line - so this line is covered by every test checking this function).
        @Override
        public void visitLineNumber(int line, Label start) {
            if ("verify".equals(methodName)
                    && Arrays.asList(context.getClassInfo().getInterfaces()).contains("javax/net/ssl/HostnameVerifier")
                    && !(context.getClassInfo().getName().startsWith("javax/net"))
                    && !mutated
                    ) {
                this.context.registerCurrentLine(line);
                final MutationIdentifier newId = this.context.registerMutation(
                        this.factory, "Changed result of HostnameVerifier to constant true");
                if (this.context.shouldMutate(newId)) {
                    this.mv.visitLineNumber(line, start);
                    this.mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_1);
                    this.mv.visitInsn(org.objectweb.asm.Opcodes.IRETURN);
                    this.mv.visitMaxs(4, 1);
                    this.mv.visitEnd();
                } else {
                    this.mv.visitLineNumber(line, start);
                }
                mutated = true;
            } else {
                this.mv.visitLineNumber(line, start);
            }
        }

/*        @Override
        public void visitCode() {
//            if () {
            if ("verify".equals(methodName)
                && Arrays.asList(context.getClassInfo().getInterfaces()).contains("javax/net/ssl/HostnameVerifier")
                && !(context.getClassInfo().getName().startsWith("javax/net"))
                ) {
//                this.context.registerCurrentLine(9);
//                final MutationIdentifier newId = this.context.registerMutation(
//                        this.factory, "Changed result of HostnameVerifier to constant true");
//                if (this.context.shouldMutate(newId)) {
                    this.target.visitCode();
                    this.target.visitInsn(org.objectweb.asm.Opcodes.ICONST_1);
                    this.target.visitInsn(org.objectweb.asm.Opcodes.IRETURN);
                    this.target.visitMaxs(4, 1);
                    this.target.visitEnd();
//                } else {
//                    this.mv = target;
//                    this.mv.visitCode();
//                    Log.getLogger().log(Level.WARNING, "ATHV: should not mutate");
//                }
            } else {
                this.mv = target;
                this.mv.visitCode();
                // Log.getLogger().log(Level.WARNING, "ATHV: not a verify() method");
            }
        }*/
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
//        if ("verify".equals(methodInfo.getName())
//                && Arrays.asList(context.getClassInfo().getInterfaces()).contains("javax/net/ssl/HostnameVerifier")
//                && !(context.getClassInfo().getName().startsWith("javax/net"))
//                ) {
            return new AlwaysTrueHostnameVerifierMethodVisitor(this, methodVisitor, context, methodInfo.getName());
//        } else {
//            return null;
//        }
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
