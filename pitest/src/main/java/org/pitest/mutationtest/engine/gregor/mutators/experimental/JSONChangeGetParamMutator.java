package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Mutator which changes parameters of JSONObject.get*() calls to other ones
 * taken from similar calls.
 *
 * Created by Krystian Kieczka on 2018-02-16.
 */
public enum JSONChangeGetParamMutator implements MethodMutatorFactory {
    JSON_CHANGE_GET_PARAM_MUTATOR;

//    public class JSONGetParamsCollectorMethodVisitor extends MethodNode {
//
//        private MutationContext context;
//        private MethodMutatorFactory factory;
//        private HashMap<String, ArrayList<String>> params;
//        private MethodVisitor mvCopy;
//
//        public JSONGetParamsCollectorMethodVisitor(MethodMutatorFactory factory, MethodInfo info,
//                                                   MutationContext context, MethodVisitor mv) {
//            super(Opcodes.ASM6, info.getAccessFlags(), info.getName(), info.getMethodDescriptor(), info.getSignature(), info.getExceptions());
//            this.context = context;
//            this.factory = factory;
//            this.mv = mv;
//            this.params = new HashMap<>();
//        }
//
//        private boolean isJSONGetCall(MethodInsnNode node) {
//            return node.owner.equals("org/json/JSONObject") && node.name.startsWith("get");
//        }
//
//        @Override
//        public void visitEnd() {
////            ListIterator<AbstractInsnNode> itr = instructions.iterator();
////            while (itr.hasNext()) {
////                AbstractInsnNode node = itr.next();
////                if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
////                        && isJSONGetCall((MethodInsnNode)node)
////                        && (node.getPrevious().getOpcode() == Opcodes.LDC)) {
////
////                    String param = (String) ((LdcInsnNode)node.getPrevious()).cst;
////                    ArrayList<String> thisMethodParams = params.get(((MethodInsnNode)node).name);
////                    if (thisMethodParams == null) {
////                        thisMethodParams = new ArrayList<>();
////                        params.put(((MethodInsnNode)node).name, thisMethodParams);
////                    }
////                    thisMethodParams.add(param);
////                }
////            }
////
////            for (String key : params.keySet()) {
////                System.out.println("Key: " + key);
////                for (String val : params.get(key)) {
////                    System.out.println("Val: " + val);
////                }
////            }
//
//            //JSONChangeGetParamMethodVisitor visitor = new JSONChangeGetParamMethodVisitor(factory, context, mvCopy, params);
//            accept(mv);
//        }
//
//
//
//    }

    public class JSONChangeGetParamMethodVisitor extends MethodVisitor {

        private MutationContext context;
        private MethodMutatorFactory factory;
        private HashMap<String, ArrayList<String>> params;
        private Object currentParamObject;

        public JSONChangeGetParamMethodVisitor(MethodMutatorFactory factory, MutationContext context,
                                               MethodVisitor mv) {
            super(Opcodes.ASM6, mv);
            this.context = context;
            this.factory = factory;
            this.currentParamObject = null;
            this.params = new HashMap<>();
        }

        private boolean isJSONGetCall(String owner, String name) {
            return owner.equals("org/json/JSONObject") && name.startsWith("get");
        }

        @Override
        public void visitLdcInsn(Object cst) {
            this.currentParamObject = cst;
            super.visitLdcInsn(cst);
        }

        private String findNewParam(String methodName) {
            for (String param : params.get(methodName)) {
                if (!param.equals(currentParamObject)) {
                    return param;
                }
            }
            return null;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            if (isJSONGetCall(owner, name)) {
                ArrayList<String> listOfParams = params.get(name);
                if (listOfParams != null && listOfParams.size() > 0) {

                    // find and replace parameter if one was found
                    String newParam = findNewParam(name);
                    if (newParam != null) {
                        MutationIdentifier newId = this.context.registerMutation(
                                this.factory, "Changed parameter of " + name + " to " + newParam);

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
                    if (!listOfParams.contains((String) currentParamObject)) {
                        listOfParams.add((String) currentParamObject);
                    }

                } else {
                    // this is first call to get*() in this function, just add the parameter to
                    // list, assuming that it is a parameter for get*(), and return without mutating
                    params.put(name, new ArrayList<String>());
                    params.get(name).add((String)currentParamObject);
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new JSONChangeGetParamMethodVisitor(this, context, methodVisitor);
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
