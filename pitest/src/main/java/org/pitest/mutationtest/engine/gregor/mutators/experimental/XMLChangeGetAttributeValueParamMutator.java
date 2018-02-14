package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

/**
 * Experimental mutator which changes parameter of XmlPullParser.getAttributeValue()
 * call to 0
 *
 * Created by Krystian Kieczka on 2018-02-14.
 */
public enum XMLChangeGetAttributeValueParamMutator implements MethodMutatorFactory {
    XML_CHANGE_GET_ATTRIBUTE_VALUE_PARAM_MUTATOR;

    public class XMLChangeGetAttributeValueParamMethodVisitor extends MethodVisitor {

        private MutationContext context;
        private MethodMutatorFactory factory;

        public XMLChangeGetAttributeValueParamMethodVisitor(MethodMutatorFactory factory,
                                                            MutationContext context, MethodVisitor mv) {
            super(Opcodes.ASM6, mv);
            this.context = context;
            this.factory = factory;
        }

        private boolean isGetAttributeValue(String owner, String name, String desc) {
            return owner.endsWith("xmlpull/v1/XmlPullParser")
                    && "getAttributeValue".equals(name)
                    && "(I)Ljava/lang/String;".equals(desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            if (itf && isGetAttributeValue(owner, name, desc)) {
                final MutationIdentifier newId = this.context.registerMutation(
                        this.factory, "Changed parameter of getAttributeValue() to 0");

                if (this.context.shouldMutate(newId)) {
                    this.mv.visitInsn(Opcodes.POP);
                    this.mv.visitLdcInsn(0);
                }
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new XMLChangeGetAttributeValueParamMethodVisitor(this, context, methodVisitor);
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
