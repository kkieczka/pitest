package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

import java.util.ArrayList;

/**
 * Mutator which changes second parameter of XmlPullParser.getAttributeValue(String, String)
 * (name of attribute) to some other string previously found in the same method.
 *
 * Created by Krystian Kieczka on 2018-02-18.
 */
public enum XMLChangeGetAttributeValueParamMutator implements MethodMutatorFactory {
    XML_CHANGE_GET_ATTRIBUTE_VALUE_PARAM_MUTATOR;

    public class XmlChangeGetAttributeValueParamMethodVisitor extends MethodVisitor {

        private MutationContext context;
        private MethodMutatorFactory factory;
        private ArrayList<String> params;
        private Object currentParamObject;

        public XmlChangeGetAttributeValueParamMethodVisitor(MethodMutatorFactory factory, MutationContext context,
                                               MethodVisitor mv) {
            super(Opcodes.ASM6, mv);
            this.context = context;
            this.factory = factory;
            this.currentParamObject = null;
            this.params = new ArrayList<>();
        }

        private boolean isXmlGetAttributeValueCall(String owner, String name, String desc) {
            return owner.endsWith("xmlpull/v1/XmlPullParser")
                    && "getAttributeValue".equals(name)
                    && "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(desc);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            this.currentParamObject = cst;
            super.visitLdcInsn(cst);
        }

        private String findNewParam() {
            for (String param : params) {
                if (!param.equals(currentParamObject)) {
                    return param;
                }
            }
            return null;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (isXmlGetAttributeValueCall(owner, name, desc)) {

                if (params.size() > 0) {

                    // find and replace parameter if one was found
                    String newParam = findNewParam();
                    if (newParam != null) {
                        MutationIdentifier newId = this.context.registerMutation(
                                this.factory, "Changed second parameter of " + name + " to " + newParam);

                        if (this.context.shouldMutate(newId)) {
                            this.mv.visitInsn(Opcodes.POP);
                            this.mv.visitLdcInsn(newParam);
                            this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }

                    // add current parameter to list, if it is unique
                    if (currentParamObject != null && !params.contains((String) currentParamObject)) {
                        params.add((String) currentParamObject);
                    }

                } else {
                    // this is first call to getAttributeValue() in this function, just add the parameter to
                    // list, assuming that it is a parameter for getAttributeValue(), and return without mutating
                    if (currentParamObject != null) {
                        params.add((String) currentParamObject);
                    }
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new XmlChangeGetAttributeValueParamMethodVisitor(this, context, methodVisitor);
    }

    @Override
    public String getGloballyUniqueId() {
        return getName();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
