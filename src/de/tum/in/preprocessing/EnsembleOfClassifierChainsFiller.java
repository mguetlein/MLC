package de.tum.in.preprocessing;

//public class EnsembleOfClassifierChainsFiller //extends AbstractClassifierChainFiller
//{
//		private final int numChains;
//	
//		public EnsembleOfClassifierChainsFiller()
//		{
//			super();
//			numChains = 10;
//		}
//	
//		public EnsembleOfClassifierChainsFiller(final Classifier baseClassifier)
//		{
//			super(baseClassifier);
//			numChains = 10;
//		}
//	
//		public EnsembleOfClassifierChainsFiller(final Classifier baseClassifier, final int numChains)
//		{
//			super(baseClassifier);
//			this.numChains = numChains;
//		}
//	
//		@Override
//		public MultiLabelInstances fillMissing(final MultiLabelInstances mli)
//		{
//			final Map<ValueCoordinate, Double> votes = getVotes(mli);
//			final MultiLabelInstances copy = mli.clone();
//			// Predict 1 where avg >= 0.5, 0 otherwise
//			for (final Entry<ValueCoordinate, Double> e : votes.entrySet())
//			{
//				final double avg = e.getValue() / numChains;
//				final ValueCoordinate vc = e.getKey();
//				final Attribute classAttribute = copy.getDataSet().attribute(vc.getFeature());
//				final int index1 = classAttribute.indexOfValue("1");
//				final int index0 = classAttribute.indexOfValue("0");
//				if (avg >= 0.5)
//				{
//					copy.getDataSet().get(vc.getInstance()).setValue(vc.getFeature(), index1);
//				}
//				else
//				{
//					copy.getDataSet().get(vc.getInstance()).setValue(vc.getFeature(), index0);
//				}
//			}
//			return copy;
//		}
//	
//		@Deprecated
//		public Matrix getMatrix()
//		{
//			throw new RuntimeException("getMatrix() no longer supported, replaced by getVotes(MultiLabelInstances mli)");
//		}
//	
//		public Map<ValueCoordinate, Double> getVotes(final MultiLabelInstances mli)
//		{
//			final Map<ValueCoordinate, Double> votes = new HashMap<ValueCoordinate, Double>();
//			// ensemble durchlaufen
//			for (int n = 0; n < numChains; n++)
//			{
//				final int order[] = getRandomOrder(mli.getLabelIndices());
//				final MultiLabelInstances copy = mli.clone();
//				for (int i = 0; i < order.length; i++)
//				{
//					int l = order[i];
//					// generate tariningSet
//					Instances train = null;
//					if (i < (order.length - 1))
//					{
//						final String className = copy.getDataSet().attribute(l).name();
//						final Remove remove = new Remove();
//						final int[] attr = Arrays.copyOfRange(order, i + 1, order.length);
//						remove.setAttributeIndicesArray(attr);
//						try
//						{
//							remove.setInputFormat(copy.getDataSet());
//						}
//						catch (final Exception e)
//						{
//							throw new RuntimeException(e);
//						}
//						try
//						{
//							train = Filter.useFilter(copy.getDataSet(), remove);
//						}
//						catch (final Exception e)
//						{
//							throw new RuntimeException(e);
//						}
//						l = train.attribute(className).index();
//					}
//					else
//					{
//						train = copy.getDataSet();
//					}
//					train.setClassIndex(l);
//					final Instances trainingSet = filter(false, l, train);
//					trainingSet.setClassIndex(l);
//					final Classifier c = getCopyOfBaseClassifier();
//					// train learn set (a1,...,an,yi+1,ym) where yi != null
//					try
//					{
//						c.buildClassifier(trainingSet);
//					}
//					catch (final Exception e1)
//					{
//						throw new RuntimeException(e1);
//					}
//					// generate predictionSet
//					final int[] missing = filterIndices(true, l, train);
//					final int att = order[i];
//					final Attribute classAttribute = copy.getDataSet().attribute(att);
//					for (final int inst : missing)
//					{
//						try
//						{
//							double prediction;
//							final ValueCoordinate coord = new ValueCoordinate(inst, att);
//	
//							final Instance instance = train.get(inst);
//							prediction = c.classifyInstance(instance);
//							final double[] dist = c.distributionForInstance(instance);
//							copy.getDataSet().get(inst).setValue(att, prediction);
//	
//							if (!votes.containsKey(coord))
//							{
//								votes.put(coord, 0.0);
//							}
//							double sum = votes.get(new ValueCoordinate(inst, att));
//							sum += dist[classAttribute.indexOfValue("1")];
//							votes.put(coord, sum);
//						}
//						catch (final Exception e)
//						{
//							throw new RuntimeException("Exception in fillMissing", e);
//						}
//	
//					}
//				}
//			}
//			return votes;
//		}
//	
//		@Override
//		protected String getTransformationName()
//		{
//			return "ECC";
//		}
//}