<?php
class M_room extends CI_Model{
	var $table 				='tbl_room';
    
	public function __construct(){
        parent::__construct();
    }

    function _insert($table,$data){
        $this->db->insert($table,$data);
    }
    function _getlokasi(){
       return  $this->db->get("lokasi");
    }

    function getdatamodel($lokasi){
        $data   =$this->db->where("tbl_room.lokasi",$lokasi)->select("*,$this->table.id as roomid,rm.nama_jenis,coalesce(sum(jumlah),0) as totalaset,coalesce(sum(jumlah*harga_assets),0) as nominalassets")
                ->from($this->table)->join('tbl_jenis_room rm',"$this->table.jenis_room=rm.id","left")     
                ->join('tbl_room_asset rma',"$this->table.id=rma.room_id","left")
                ->group_by("$this->table.id")->order_by('nomor_room','ASC');     
        return $data->get();
    }
    function getdatamodelkon($get,$lokasi){
        $data   =$this->db->where("tbl_room.lokasi",$lokasi)->select("*,$this->table.id as roomid,rm.nama_jenis,coalesce(sum(jumlah),0) as totalaset,coalesce(sum(jumlah*harga_assets),0) as nominalassets")
                ->from($this->table)->join('tbl_jenis_room rm',"$this->table.jenis_room=rm.id","left")     
                ->join('tbl_room_asset rma',"$this->table.id=rma.room_id","left")
                ->like("$this->table.kode_room",$get)
                ->or_like("$this->table.nomor_room",$get)
                ->or_like("$this->table.nomor_room",$get)
                ->group_by("$this->table.id")->order_by('nomor_room','ASC');     
        return $data->get();
    }
    function getdataRoom($lokasi){
        return $this->db->select('*')->from('tbl_room')->where('lokasi',$lokasi)->where('status_room','0')->order_by('nomor_room','ASC')->get();
    }
    function getassetfaslilitas($rom_ids){
        $data = $this->db->select('*')
                ->from('tbl_room_asset')
                ->join('tbl_assets','tbl_room_asset.asset_id=tbl_assets.id','LEFT')
                ->where(
                    array('tbl_room_asset.room_id'  =>$rom_ids,
                         'tbl_assets.type_asset'    =>'1'
                        )
                    );
        return $this->db->get();
    }
    function getassetfaslilitasbyno($rom_ids){
        $data = $this->db->select('*')
                ->from('tbl_room_asset')
                ->join('tbl_assets','tbl_room_asset.asset_id=tbl_assets.id','LEFT')
                ->join('tbl_room','tbl_room_asset.room_id=tbl_room.id','LEFT')
                ->where(
                    array('tbl_room.nomor_room'  =>$rom_ids,
                         'tbl_assets.type_asset'    =>'1'
                        )
                    );
        return $this->db->get();
    }
    function getdataassetdetail($type,$id){
        if($type=="noroom"){
            $datas = array("$this->table.id"=>$id);
        }else{
            $datas = array("rma.asset_id"=>$id);
        }
        $data   =$this->db->select("*,rma.*,rma.id as assetid,assetx.nama_asset,$this->table.id as roomid,rm.nama_jenis,coalesce(sum(jumlah),0) as totalaset,coalesce(sum(jumlah*harga_assets),0) as nominalassets")
        ->from($this->table)->join('tbl_jenis_room rm',"$this->table.jenis_room=rm.id","left")     
        ->join('tbl_room_asset rma',"$this->table.id=rma.room_id","left")
        ->join('tbl_assets assetx',"rma.asset_id=assetx.id","left")->where($datas)->group_by("rma.id");     
        return $data->get();
    }
    function getdataassets(){
        return $this->db->get('tbl_assets');
    }
    function _hapus($table,$data){
        $this->db->where($data)
        ->delete($table);
    }
    function gettablebyid($id){
        $this->db->select('*')
        ->from($this->table)
        ->where('id',$id);
        $data   =$this->db->get();
        return $data->row();
    }
    function getleveluserbyid($id){
        $this->db->select('*')
        ->from($this->table)
        ->where('level_id',$id);
        $data   =$this->db->get();
        return $data->row();
    }
    function _updates($data,$where){
        $this->db->where($where)->update($this->table,$data);
        
    }
    function gettyperoom(){
        return $this->db->get('room_type');
    }
    function getsubdivisi(){
        $this->db->select('division.*,dep.*')
        ->from('subdivisi division')
        ->join('departement dep','division.id_divisi=dep.departement_id','LEFT');
        return $this->db->get();
    }
    function getdatatamu($lokasi){
        $this->db->select('tbl_check_in.*,tbl_sales.nama_sales')
        ->from('tbl_check_in')
        ->join('tbl_sales','tbl_sales.id=tbl_check_in.sales')
        ->where(array('status_checkout'=>'0','tbl_check_in.lokasi'=>$lokasi));
        return $this->db->get();
    }
   public function gettiperoombynomor($nomorrom)
   {
    return $this->db->select('tbl_room.nomor_room,tbl_jenis_room.nama_jenis')
    ->from('tbl_room')
    ->join('tbl_jenis_room','tbl_jenis_room.id=tbl_room.jenis_room')
    ->where(array('tbl_room.nomor_room'=>$nomorrom))->get();
   }
   public function gettablebayar()
   {
    return $this->db->select('*')->from('tbl_bank_pembayaran')->get();
   }
   public function getpesan($nomorrom)
   {
    return $this->db->get_where('tbl_notes_room',array('room_no'=>$nomorrom,'status'=>'0'));
    # code...
   }
   public function getinformasiroom($koderoom,$tgl)
   {

    return $this->db->select('*')
    ->from('room_cleaning')
    ->where(array('no_room'=>$koderoom,'status'=>'S','create_date<='=>$tgl))
    ->order_by('id','desc')
    ->get()
    ->row();
    # code...   
   }
}
