import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Historico
import com.projetointegrador.reuse.databinding.CardviewTransacaoBinding
import com.projetointegrador.reuse.util.displayBase64Image


class HistoricoAdapter (
    private val historicoList: MutableList<Historico>,
    // 🛑 CORREÇÃO: O callback agora aceita apenas uma String (avaliacaoUID)
    private val onAvaliarClicked: (avaliacaoUID: String) -> Unit
): RecyclerView.Adapter<HistoricoAdapter.MyViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewTransacaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = historicoList.size

    fun updateList(newList: List<Historico>) {
        historicoList.clear()
        historicoList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val historicoItem = historicoList[position]

        // 1, 2, 3: (Exibição de dados e visibilidade do botão)
        if (historicoItem.fotoBase64.isNotEmpty()) {
            displayBase64Image(historicoItem.fotoBase64, holder.binding.imgProduto)
        } else {
            holder.binding.imgProduto.setImageResource(R.drawable.closeticon)
        }
        holder.binding.tvNomeProduto.text = historicoItem.name
        holder.binding.tvDescricao.text = historicoItem.description
        holder.binding.bttAdd.visibility = if (historicoItem.button) View.VISIBLE else View.GONE


        // 4. Configurar o clique no botão
        if (historicoItem.button) {
            holder.binding.bttAdd.setOnClickListener {
                // 🛑 CORREÇÃO: Passa apenas o avaliacaoUID
                onAvaliarClicked(historicoItem.avaliacaoUID)
            }
        } else {
            holder.binding.bttAdd.setOnClickListener(null)
        }
    }

    inner class MyViewHolder(val binding : CardviewTransacaoBinding): RecyclerView.ViewHolder(binding.root){

    }
}