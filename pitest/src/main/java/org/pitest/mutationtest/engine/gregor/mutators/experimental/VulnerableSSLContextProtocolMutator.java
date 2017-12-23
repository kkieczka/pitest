package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.util.Log;

import java.util.logging.Level;

/**
 * The mutator that changes the 'protocol' parameter in invocations of
 * SSLContext.getInstance() methods to SSLv3, which is currently
 * considered insecure.
 *
 * Created by Krystian Kieczka on 2017-11-20.
 */
public enum VulnerableSSLContextProtocolMutator implements MethodMutatorFactory {

    VULNERABLE_SSL_CONTEXT_PROTOCOL_MUTATOR;

    public class VulnerableSSLContextProtocolMethodVisitor extends MethodVisitor {

        private MutationContext context;
        private MethodMutatorFactory factory;

        public VulnerableSSLContextProtocolMethodVisitor(MethodMutatorFactory factory,
                                                         MutationContext context, MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
            this.context = context;
            this.factory = factory;
        }

        private boolean shouldMutate(String methodOwner, String methodName) {
            return "javax/net/ssl/SSLContext".equals(methodOwner) && "getInstance".equals(methodName);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            // System.out.println("Inside " + owner + ": " + desc);
            if (opcode == Opcodes.INVOKESTATIC && shouldMutate(owner, name)) {

                if ("(Ljava/lang/String;)Ljavax/net/ssl/SSLContext;".equals(desc)) {
                    // remove parameter (usually 'TLS' or 'Default') from stack and put 'SSLv3' there
                    this.mv.visitInsn(Opcodes.POP);
                    this.mv.visitLdcInsn("SSLv3");
                    final MutationIdentifier newId = this.context.registerMutation(
                            this.factory, "Changed parameter of SSLContext.getInstance() to 'SSLv3'");

                } else if ("(Ljava/lang/String;Ljava/lang/String;)Ljavax/net/ssl/SSLContext;".equals(desc)
                        || "(Ljava/lang/String;Ljava/security/Provider;)Ljavax/net/ssl/SSLContext;".equals(desc)) {

                    // current operand stack state: [..., 'TLS', op2]
                    this.mv.visitInsn(Opcodes.SWAP);    // op stack state: [..., op2, 'TLS']
                    this.mv.visitInsn(Opcodes.POP);     // op stack state: [..., op2]
                    this.mv.visitLdcInsn("SSLv3");  // op stack state: [..., op2, 'SSLv3']
                    this.mv.visitInsn(Opcodes.SWAP);    // op stack state: [..., 'SSLv3', op2]

                    final MutationIdentifier newId = this.context.registerMutation(
                            this.factory, "Changed first parameter of SSLContext.getInstance() to 'SSLv3'");
                } else {
                    Log.getLogger().log(Level.WARNING, "Unknown variant of SSLContext.getInstance(): " + desc);
                }
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new VulnerableSSLContextProtocolMethodVisitor(this, context, methodVisitor);
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
