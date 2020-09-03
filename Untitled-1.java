
	public boolean Write(String path, String texto){
		try{
			FileWriter arq = new FileWriter(path);
			PrintWriter gravarArq = new PrintWriter(arq);
			gravarArq.println(texto);
			gravarArq.close();
			return true;
		}catch(IOException e){
			System.out.println(e.getMessage());
			return false;
		}
  }
  

		// List<String> lista = new ArrayList<String>();
		// String linha = "";
		// 	String path = "./usuariosAtivos.txt";
		// 	File arq = new File(path);
		// 	if(arq.exists()){
		// 		try{
		// 			FileReader leitorArquivo = new FileReader(path);
		// 			BufferedReader bufferArquivo = new BufferedReader(leitorArquivo);

		// 			while(true){
		// 				linha = bufferArquivo.readLine();

		// 				if(linha != null){
		// 					lista.add(linha);
		// 				}
		// 				if(linha == null){
		// 					leitorArquivo.close();
		// 					break;
		// 				}
		// 			}
		// 		}catch(IOException e){
		// 			System.out.println("Não foi possivel ler o arquivo!");
		// 		}
    // 	}  
    

				// byte[] bytes = baos.toByteArray();
				// ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				// ObjectInputStream ois = new ObjectInputStream(bais);
				// Mensagem asp = (Mensagem) ois.readObject();


					// System.out.println("lista tamanho: " + lista.size());
					
					// int contador = 0;
					// 	for (String item : lista) {
					// 		if (item.equals(MulticastPeer.source)){
					// 			contador += 1; 
					// 		}
					// 	}
					// System.out.println("contador: " + contador);
					// System.out.println("source: "+ objDescerializado.getSource());
						
					// 	//Função utilizada para atualizar a lista do usuario que entrou
					// 	if(contador == 0 && flag == 1){
					// 		lista.add(objDescerializado.getSource());
					// 		System.out.println("flag 1 "+lista);
					// 		flag = 0;
					// 	}
					// 	else if(contador == 0){
					// 		System.out.println("JOINACK " + objDescerializado.getSource());
					// 		lista.add(objDescerializado.getSource());
					// 		flag = 1;
					// 		// Pacote para atualização da lista do usuário adicionado
					// 		obj = MulticastPeer.serializacao(new Mensagem((byte) 2,MulticastPeer.source,"",MulticastPeer.source.length(),0)); //Java não define escopo da variavel pelo switch case, estranho
					// 	  messageOut = new DatagramPacket(obj, obj.length, group, 6789);
					// 		multicastSocket.send(messageOut);
					// 		System.out.println("cntd = 0 " + lista);
					// 	}