package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

/**
 * The mutator which replaces 'https' with 'http' at the beginnings of Strings.
 *
 * Created by Krystian Kieczka on 2018-01-03.
 */
public enum HttpsToHttpMutator implements MethodMutatorFactory {
    HTTPS_TO_HTTP_MUTATOR;

    public class HttpsToHttpInURLMethodVisitor extends MethodVisitor {

        private MutationContext context;
        private MethodMutatorFactory factory;

        public HttpsToHttpInURLMethodVisitor(MethodMutatorFactory factory,
                                             MutationContext context, MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
            this.context = context;
            this.factory = factory;
        }

        @Override
        public void visitLdcInsn(Object cst) {

            if (cst instanceof String) {

                String arg = (String)cst;

                if (arg.toLowerCase().startsWith("https")) {
                    String modified = "http" + (arg.length() > 5 ? arg.substring(5) : "");

                    final MutationIdentifier newId = this.context.registerMutation(
                            this.factory, "Changed 'https' to 'http' in String");
                    if (this.context.shouldMutate(newId)) {
                        this.mv.visitLdcInsn(modified);
                    } else {
                        this.mv.visitLdcInsn(cst);
                    }

                } else {
                    super.visitLdcInsn(cst);
                }

            } else {
                super.visitLdcInsn(cst);
            }
        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new HttpsToHttpInURLMethodVisitor(this, context, methodVisitor);
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
